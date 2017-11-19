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
    //TODO: per ogni agente errore totale e errore su città (non è meglio errore su task? Meno complicato. Sì, ma meno preciso

    private Map<Integer, AgentStatus> agentStatusMap;

    // This map contains for every agent the prediction of his bid
    private Map<Integer, Double> agentPredictionMap;

    private double currentMarginalCost;
    //TODO: ad ogni turno aggiornarlo per non doverlo calcolare due volte

    private double balance;
    //TODO: aggiornare ogni volta che vinco una task

    private double balanceThreshold;
    //TODO: variabile che viene usata per avere una strategia aggressiva o meno

    private boolean firstAuction = true;

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

                if (!pickUpCity.equals(deliveryCity)) {

                    double prob = taskDistribution.probability(pickUpCity, deliveryCity);

                    if(prob >= probabilityBound){
                        int reward = taskDistribution.reward(pickUpCity, deliveryCity);
                        int weight = taskDistribution.weight(pickUpCity, deliveryCity);
                        taskProbabilityMap.put(new Task(id, pickUpCity, deliveryCity, reward, weight), prob);
                    }
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
    //TODO: perchè calcolare il marginal cost di quelle già vinte? Lo si fa già al turno prima, se poi si vince effetivamente la task si aggiorna il MC attuale (che diventa un attributo)
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


    /**
     * Predicting the bids of every other agent.
     * @param task
     */
    public void predictBids(Task task) {

        for (Map.Entry<Integer, AgentStatus> agentStatusEntry : agentStatusMap.entrySet()) {

            Integer enemy = agentStatusEntry.getKey();
            AgentStatus enemyStatus = agentStatusEntry.getValue();

            //TODO: non possiamo calcolare la strategia dell'avversario, non abbiamo i suoi veicoli

            // Here we compute the prediction of the bid - marginal cost of an agent
            double prediction = mCostPrediction(enemyStatus, task);

            // We add this prediction to the map. We will use it later to compare our prediction with the actual bid
            agentPredictionMap.put(enemy, prediction);

        }

    }

    /**
     * Marginal cost prediction for one agent.
     * If the agent never bid before, we use our bid as prediction.
     * If the agent bid at least once, we can face three different cases:
     *
     * 1) None of the cities of the task of the auction has a previous offer for that agent. We are going to use the
     * average of his bids as prediction
     *
     * 2) One of the two cities has a previous offers: we use that as a prediction
     *
     * 3) Both cities have previous offers: we use the average of them to compute our prediction
     * @param enemyStatus
     * @param task
     * @return
     */
    private double mCostPrediction(AgentStatus enemyStatus, Task task) {

        double prediction = 0;
        int div = 0;

        // If the enemy never bid, the prediction will be equal to my offer
        if (!enemyStatus.hasAlreadyBid()) {

            // TODO: si salva da qualche parte l'offerta che voglio fare?
            prediction = MYBID;
            div = 1;
        }

        else {
            Topology.City pickupCity = task.pickupCity;
            Topology.City deliveryCity = task.deliveryCity;

            Map<Topology.City, List<Double>> pickupCityBids = enemyStatus.getPickUpCitiesBids();
            Map<Topology.City, List<Double>> deliveryCityBids = enemyStatus.getDeliveryCitiesBids();

            // If there are old offers for that pickUp City, we use them to make our prediction
            if (pickupCityBids.keySet().contains(pickupCity)) {
                OptionalDouble average = pickupCityBids.get(pickupCity).stream().mapToDouble(pred -> pred).average();
                if (average.isPresent()) {
                    prediction += average.getAsDouble();
                }
                div += 1;
            }

            // If there are old offers for that delivery City, we use them to make our prediction
            if (deliveryCityBids.keySet().contains(deliveryCity)) {
                OptionalDouble average = deliveryCityBids.get(deliveryCity).stream().mapToDouble(pred -> pred).average();
                if (average.isPresent()) {
                    prediction += average.getAsDouble();
                }
                div += 1;
            }

            // This means that we didn't find any offer for those cities
            // We have to use the mean of the agent bids
            if (prediction == 0) {
                prediction = enemyStatus.getAverageBid();
                div = 1;
            }
        }

        return prediction/div;

    }


    /**
     * Marginal cost prediction for one agent.
     * First we compute his offer using our strategy.
     * Secondly we compute the errors we made for every past prediction.
     * We return the prediction and a range = prediction +- error
     *
     * @param enemyStatus
     * @param task
     * @return
     */
    private Double[] mCostPrediction2(AgentStatus enemyStatus, Task task) {

        // First we predict the marginal cost following our strategy
        // TODO: pensare a come gestire lista invece di TaskSet
        double prediction = presentMarginalCost(task, enemyStatus.getTasksWon());

        Topology.City pickupCity = task.pickupCity;
        Topology.City deliveryCity = task.deliveryCity;

        List<Double> pickupCityPredictions = enemyStatus.getPickUpCitiesPredictions().get(pickupCity);
        List<Double> deliveryCityPredictions= enemyStatus.getDeliveryCitiesPredictions().get(deliveryCity);

        double error = 0;

        // If the enemy never bid, the prediction will be equal to my offer
        // Useless if
        if (!enemyStatus.hasAlreadyBid()) {

            Double[] range = new Double[2];
            range[0] = prediction - error;
            range[1] = prediction + error;
            return range;
        }

        else {

            Map<Topology.City, List<Double>> pickUpCitiesBids = enemyStatus.getPickUpCitiesBids();
            Map<Topology.City, List<Double>> deliveryCityBids = enemyStatus.getDeliveryCitiesBids();


            int numberOfBids = 0;

            // If there are old offers for that pickUp City, we use their average to make our prediction
            if (pickUpCitiesBids.keySet().contains(pickupCity)) {

                List<Double> puBids = pickUpCitiesBids.get(pickupCity);


                // For every auction, we compute the abs error and then we take the average of them
                for (int i = 0; i < puBids.size(); i++) {

                    // It can happen that an agent makes a null offer
                    if (puBids.get(i) != null) {
                        error += Math.abs(puBids.get(i) - pickupCityPredictions.get(i));
                        numberOfBids++;
                    }
                }
            }

            // If there are old offers for that delivery City, we use their average to make our prediction
            if (deliveryCityBids.keySet().contains(deliveryCity)) {

                List<Double> dBids = deliveryCityBids.get(deliveryCity);
                for (int i = 0; i < dBids.size(); i++) {

                    // It can happen that an agent makes a null offer
                    if (dBids.get(i) != null) {
                        error += Math.abs(dBids.get(i) - deliveryCityPredictions.get(i));
                        numberOfBids++;
                    }
                }
            }

            // error is the average error that we did in our predictions
            error /= numberOfBids;
        }

        // Adding our predictions to the enemy status
        pickupCityPredictions.add(prediction);
        deliveryCityPredictions.add(prediction);
        Double[] range = new Double[2];
        range[0] = prediction - error;
        range[1] = prediction + error;

        return range;

    }

    /**
     * After the auction is completed, we have to update the agents' status.
     * First we add the bids they made (if they made one).
     * Secondly we compute our error we made with our prediction compared to the real offer.
     * Third we add the task to the winner of the auction.
     * @param lastTask
     * @param lastOffers
     * @param lastWinner
     */
    public void auctionCompleted(Task lastTask, Long[] lastOffers, int lastWinner) {

        Topology.City pickUpCity = lastTask.pickupCity;
        Topology.City deliveryCity = lastTask.deliveryCity;

        for (int enemy = 0; enemy < lastOffers.length; enemy++) {

            AgentStatus enemyStatus = agentStatusMap.get(enemy);

            // If the enemy participated to the auction we update his bids in his status
            if (lastOffers[enemy] != null) {

                Map<Topology.City, List<Double>> pickUpCitiesBids = enemyStatus.getPickUpCitiesBids();
                Map<Topology.City, List<Double>> deliveryCitiesBids = enemyStatus.getDeliveryCitiesBids();


                updateOffers(pickUpCitiesBids, pickUpCity, lastOffers[enemy].doubleValue());
                updateOffers(deliveryCitiesBids, deliveryCity, lastOffers[enemy].doubleValue());

                // Here we compute the error we made and we add it to the errors of the status
                double error  = Math.abs(agentPredictionMap.get(enemy) - lastOffers[enemy].doubleValue());
                enemyStatus.getErrors().add(error);
            }

            if (enemy == lastWinner) {
                enemyStatus.getTasksWon().add(lastTask);
            }


        }

    }

    /**
     * Update the offers made by the agent
     * @param previousBids
     * @param city
     * @param offer
     */
    public void updateOffers(Map<Topology.City, List<Double>> previousBids, Topology.City city, double offer) {

        List<Double> cityBids = previousBids.get(city);

        // If the enemy already made bids for that city we add the offer
        if (cityBids != null) {
            cityBids.add(offer);

        }
        // Otherwise, we create the entry with the first offer
        else {
            List<Double> offers = new ArrayList<>();
            offers.add(offer);
            previousBids.put(city, offers);
        }
    }

}
