package template;

import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

/**
 * Created by Christian on 17/11/2017.
 */
public class AuctionStrategy {

    private TaskDistribution taskDistribution;
    private Topology topology;
    private Agent agent;
    private long timeoutBid;
    private Map<Task, Double> taskProbabilityMap;
    private Map<Task, Double> neededTasksMap;
    private Map<Task, Double> temporaryNeededTasksMap;

    //map for the tasks won by the other agents.
    private Map<Integer, List<Task>> agentsTasksMap;
    //TODO: per ogni agente errore totale e errore su città (non è meglio errore su task? Meno complicato)


    /**
     *
     * @param taskDistribution
     * @param timeoutBid
     */
    public AuctionStrategy(TaskDistribution taskDistribution, long timeoutBid, Agent agent, Topology topology) {
        this.taskDistribution = taskDistribution;
        this.agent = agent;
        this.timeoutBid = timeoutBid;
        this.topology = topology;

        taskProbabilityMap = new HashMap<>();
        neededTasksMap = new HashMap<>();
        temporaryNeededTasksMap = new HashMap<>();

        createTasks(topology, taskDistribution, 0.10);

    }

    /**
     * For each pair combination of cities in the topology we create a task with fake ID.
     * We create the task only if the probability to find a task for that combination of cities
     * is higher than a probabilityBound.
     */
    private void createTasks(Topology topology, TaskDistribution taskDistribution, double probabilityBound) {

        int id = 1000;
        for (Topology.City pickUpCity : topology.cities()) {
            for (Topology.City deliveryCity : topology.cities()) {

                double prob = taskDistribution.probability(pickUpCity, deliveryCity);

                if(prob >= probabilityBound){
                    int reward = taskDistribution.reward(pickUpCity, deliveryCity);
                    int weight = taskDistribution.weight(pickUpCity, deliveryCity);
                    taskProbabilityMap.put(new Task(id, pickUpCity, deliveryCity, reward, weight), prob);
                }
            }
        }

    }


    /**
     * evaluates the marginal cost for the task in the current auction
     * without considering the future and the opponents' strategies.
     * It computes the best solution for the agent with SLS considering only
     * the task that it won, then a best solution considering also the current
     * task. After this, it returns the difference of the two planCosts.
     */
    public double presentMarginalCost(Task task, TaskSet presentTaskSet){

        CompanyStrategy presentCompanyStrategy = new CompanyStrategy(presentTaskSet, agent.vehicles());
        Solution bestSolution = presentCompanyStrategy.SLS(5000, timeoutBid, 0.35, 100, presentCompanyStrategy.initialSolution());

        TaskSet tempTaskSet = presentTaskSet;
        tempTaskSet.add(task);
        CompanyStrategy tempCompanyStrategy = new CompanyStrategy(tempTaskSet, agent.vehicles());

        Solution tempBestSolution = tempCompanyStrategy.addTask(bestSolution, task);
        tempBestSolution = tempCompanyStrategy.SLS(5000, timeoutBid, 0.35, 100, tempBestSolution);

        return bestSolution.objectiveFunction() - tempBestSolution.objectiveFunction();
    }

    /**
     * updates the present marginal cost considering future tasks
     */
    public double futureMarginalCost(Task task, double presentMarginalCost, double costBound){

        /*
            TODO: si può inserire intervallo futuro, con questo intervallo 'n'
            considero solo i primi 'n' task con probabilità più alta.
         */

        Map<Task, Double> marginalCostFutureTasks = new HashMap<>();

        /*
         for each task that we have created, we evaluate the marginCost
         of the current task in the auction, considering that we have won
         also the futureTask
         */
        for (Task futureTask : taskProbabilityMap.keySet()) {

            TaskSet taskSet = agent.getTasks();
            taskSet.add(futureTask);

            // here we evaluate the futureMarginalCost of the current task
            double futureMarginalCost = presentMarginalCost(task, taskSet);

            // the future marginal costs higher than the costBound are discarded
            if(futureMarginalCost < costBound){
                marginalCostFutureTasks.put(futureTask, futureMarginalCost);
            }

        }

        double weightedMarginalCost = 0.;
        double probSum = 0.;

        /*
            we compute the weighted mean of the futureMarginalCosts
            (using as weight the probability of find that task).
         */
        for (Map.Entry<Task, Double> taskDoubleEntry : marginalCostFutureTasks.entrySet()) {

            Task futureTask = taskDoubleEntry.getKey();
            double futureMarginalCost = taskDoubleEntry.getValue();

            double taskProb = taskProbabilityMap.get(futureTask);

            weightedMarginalCost += taskProb * futureMarginalCost;
            probSum += taskProb;

        }

        // here, we update the presentMarginalCost with the weightedMarginalCost
        double updatedMarginalCost = presentMarginalCost - weightedMarginalCost/probSum;

        /*
            here we create a temporary map of all the futureTask that
            were taken into account for evaluating the weightedMarginalCost
         */
        createTemporaryNeededTasks(marginalCostFutureTasks, updatedMarginalCost);

        return updatedMarginalCost;
    }

    /**
     * creates a temporary map of all the futureTask that
     * were taken into account for evaluating the weightedMarginalCost.
     * The values of the map are the ifference between the presentMarginalCost
     * and the futureMarginalCost.
     */
    private void createTemporaryNeededTasks(Map<Task, Double> marginalCostFutureTasks, double updatedMarginalCost) {

        for (Map.Entry<Task, Double> taskDoubleEntry : marginalCostFutureTasks.entrySet()) {

            Task task = taskDoubleEntry.getKey();
            double marginalCost = taskDoubleEntry.getValue();

            temporaryNeededTasksMap.put(task, updatedMarginalCost - marginalCost);
        }
    }

    //TODO: da chiamare quando ti viene detto che hai vinto il task

    /**
     * nelle due mappe di neededTask i task avranno sempre l'ID che ho creato io, dunque puoi usare contains.
     * Devi stare attento quando poi riutilizzi la neededTask nel riabbasso della bid.
     *
     * Called only if the agent won the current task, it saves the important future tasks
     * for the current task. It saves the maximum difference between the presentMarginalCost
     * and the futureMarginalCost.
     */
    private void updateNeededTasks(){

        Set<Task> tasks = neededTasksMap.keySet();

        for (Map.Entry<Task, Double> taskDoubleEntry : temporaryNeededTasksMap.entrySet()) {

            Task task = taskDoubleEntry.getKey();
            double tempDifferenceMarginCost = taskDoubleEntry.getValue();

            /*
             tengo conto che il task futuro sia già stato considerato, salvo il
             margine di guadagno più alto
             */
            if(tasks.contains(task)){

                double diffMarginCost = neededTasksMap.get(task);

                neededTasksMap.put(task, Math.max(diffMarginCost, tempDifferenceMarginCost));

            }
            // se non c'è salvo il task futuro importante e il margine di guadagno.
            else {
                neededTasksMap.put(task, tempDifferenceMarginCost);
            }
        }
    }

    /**
     * evaluates a prevision bid of all opponents for the current task,
     */
    public double minBidPrevision(Task task){

        return 0.;
    }

    /**
     * updates the current bid taking into account the opponents'
     * strategies.
     */
    public double opponentStrategiesUpdate(double currentBid, Task task){

        return 0.;
    }

    /**
     * computes the bid for the task in the current auction
     */
    public double evaluateBid(Task task){

        return 0.;
    }

    //TODO: creare metodi per fare gli update delle mappe / liste di questa classe.

}
