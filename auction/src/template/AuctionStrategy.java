package template;

import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.simulation.VehicleImpl;
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
    private List<Map.Entry<Task, Double>> orderedTaskProbList;
    private Map<Task, Double> neededTasksMap;
    private Map<Task, Double> temporaryNeededTasksMap;

    private int auctionNumber = 0;

    private Map<Integer, AgentStatus> agentStatusMap;

    // This map contains for every agent the prediction of his bid
    //private Map<Integer, Double> agentPredictionMap;

    private Map<Integer, Solution> currentBestSolutionMap;
    private Map<Integer, Solution> temporaryBestSolutionMap;
    //TODO: ad ogni turno aggiornarlo per non doverlo calcolare due volte

    private double balance;
    //TODO: aggiornare ogni volta che vinco una task - serve costo per km

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

        agentStatusMap = new HashMap<>();
        taskProbabilityMap = new HashMap<>();
        neededTasksMap = new HashMap<>();
        temporaryNeededTasksMap = new HashMap<>();
        currentBestSolutionMap = new HashMap<>();
        temporaryBestSolutionMap = new HashMap<>();

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

        createOrderedTasksList();

    }

    private void createOrderedTasksList() {

        orderedTaskProbList = new ArrayList<>(taskProbabilityMap.entrySet());
        Collections.sort( orderedTaskProbList, new Comparator<Map.Entry<Task, Double>>() {
            public int compare(Map.Entry<Task, Double> o1, Map.Entry<Task, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
    }


    /**
     * evaluates the marginal cost for the task in the current auction
     * without considering the future and the opponents' strategies.
     * It computes the best solution for the agent with SLS considering only
     * the task that it won, then a best solution considering also the current
     * task. After this, it returns the difference of the two planCosts.
     */
    //TODO: se return negativo, scartare task
    //TODO: le solution vanno salvate solo nelle variabili di classe quando si tratta del mio agente. --> RISOLTO
    //TODO: crea metodo per ricercare task nella Solution, per sapere a quale veicolo è stato assegnato --> RISOLTO
    public double presentMarginalCost(Task task, TaskSet presentTaskSet, List<Vehicle> vehicles, int agentID){

        CompanyStrategy presentCompanyStrategy = new CompanyStrategy(presentTaskSet, vehicles);
        if(!currentBestSolutionMap.keySet().contains(agentID)){
            currentBestSolutionMap.put(agentID, presentCompanyStrategy.SLS(5000, timeoutBid,0.35,
                                                             100, presentCompanyStrategy.initialSolution()));
        }

        TaskSet tempTaskSet = presentTaskSet;
        tempTaskSet.add(task);
        CompanyStrategy tempCompanyStrategy = new CompanyStrategy(tempTaskSet, vehicles);

        Solution temporary = tempCompanyStrategy.addTask(currentBestSolutionMap.get(agentID), task);
        temporaryBestSolutionMap.put(agentID, tempCompanyStrategy.SLS(5000, timeoutBid, 0.35, 100, temporary));

        return temporaryBestSolutionMap.get(agentID).objectiveFunction() - currentBestSolutionMap.get(agentID).objectiveFunction();
    }

    /**
     * updates the present marginal cost considering future tasks
     */
    public double futureMarginalCost(Task task, double presentMarginalCost, double costBound, int futureTasks){

        /*
            TODO: si può inserire intervallo futuro, con questo intervallo 'n' --> RISOLTO
            considero solo i primi 'n' task con probabilità più alta.
         */

        Map<Task, Double> marginalCostFutureTasks = new HashMap<>();

        /*
         for each task that we have created, we evaluate the marginCost
         of the current task in the auction, considering that we have won
         also the futureTask
         */
        for (Task futureTask : getTasksByProbability(futureTasks)) {

            TaskSet taskSet = agent.getTasks();
            taskSet.add(futureTask);

            // here we evaluate the futureMarginalCost of the current task
            double futureMarginalCost = presentMarginalCost(task, taskSet, agent.vehicles(), agent.id());

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
        //createTemporaryNeededTasks(marginalCostFutureTasks, updatedMarginalCost);

        temporaryNeededTasksMap = marginalCostFutureTasks;

        return updatedMarginalCost;
    }

    /**
     * returns the first 'numberOfTask' tasks with highest probability
     */
    //TODO: numberOfTask deve essere dipendente dal numero del turno dell'asta
    private Set<Task> getTasksByProbability(int numberOfTask){

        Set<Task> tasks = new HashSet<>();
        for (int index = 0; index < numberOfTask; index++) {
            tasks.add(orderedTaskProbList.get(index).getKey());
        }

        return tasks;
    }

    /**
     * creates a temporary map of all the futureTask that
     * were taken into account for evaluating the weightedMarginalCost.
     * The values of the map are the difference between the presentMarginalCost
     * and the futureMarginalCost.
     */
    private void createTemporaryNeededTasks(Map<Task, Double> marginalCostFutureTasks, double updatedMarginalCost) {

        for (Map.Entry<Task, Double> taskDoubleEntry : marginalCostFutureTasks.entrySet()) {

            Task task = taskDoubleEntry.getKey();
            double marginalCost = taskDoubleEntry.getValue();

            //TODO: cambiare, usare 500, non 300 o 200 --> RISOLTO
            temporaryNeededTasksMap.put(task, updatedMarginalCost - marginalCost);
        }
    }

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

    /**
     * Predicting the bids of every other agent.
     * Returns the minimum bid
     * @param task
     */
    public Double predictBids(Task task) {

        Double min = Double.POSITIVE_INFINITY;

        // For every agent we predict the range of bid

        for (Map.Entry<Integer, AgentStatus> agentStatusEntry : agentStatusMap.entrySet()) {
            AgentStatus enemyStatus = agentStatusEntry.getValue();
            int agentID = agentStatusEntry.getKey();

            //TODO: controllare che non sia il mio agente - dovrebbe essere ok

            // Here we compute the prediction of the bid - marginal cost of an agent
            List<Double> range = mCostPrediction2(enemyStatus, agentID, task);
            Double minRange = range.get(0);

            // We save the id of the enemy that made the minimum bid
            if (minRange < min) {
                min = minRange;
            }

        }

        /*
        for (AgentStatus enemyStatus : agentStatusMap.values()) {

            //TODO: controllare che non sia il mio agente - dovrebbe essere ok

            // Here we compute the prediction of the bid - marginal cost of an agent
            List<Double> range = mCostPrediction2(enemyStatus, task);
            Double minRange = range.get(0);

            // We save the id of the enemy that made the minimum bid
            if (minRange < min) {
                min = minRange;
            }

        }*/

        return min;

    }

/*    *//**
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
     *//*
    private double mCostPrediction(AgentStatus enemyStatus, Task task) {

        double prediction = 0;
        int div = 0;

        // If the enemy never bid, the prediction will be equal to my offer
        if (!enemyStatus.hasAlreadyBid()) {

            // TODO: si salva da qualche parte l'offerta che voglio fare?
            *//*prediction = MYBID;*//*
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

    }*/


    /**
     * Marginal cost prediction for one agent.
     * First we compute his offer using our strategy.
     * Secondly we compute the average of the errors we made for every past prediction in the cities of the task.
     * Finally we return  a range = prediction +- averageError, where averageError is the averageError we do
     * when we predict for that agent
     *
     * @param enemyStatus
     * @param task
     * @return
     */
    private List<Double> mCostPrediction2(AgentStatus enemyStatus, int agentID, Task task) {

        // First we predict the marginal cost following our strategy

        List<Vehicle> vehicles2 = createVehicles(agent.vehicles(), 2);
        List<Vehicle> vehicles3 = createVehicles(agent.vehicles(), 3);
        List<Vehicle> vehicles4 = createVehicles(agent.vehicles(), 4);
        List<Vehicle> vehicles5 = createVehicles(agent.vehicles(), 5);

        double prediction2 = presentMarginalCost(task, enemyStatus.getTasksWon(), vehicles2, agentID);
        double prediction3 = presentMarginalCost(task, enemyStatus.getTasksWon(), vehicles3, agentID);
        double prediction4 = presentMarginalCost(task, enemyStatus.getTasksWon(), vehicles4, agentID);
        double prediction5 = presentMarginalCost(task, enemyStatus.getTasksWon(), vehicles5, agentID);

        double prediction = (prediction2 + prediction3 + prediction4 + prediction5)/4;

        Topology.City pickupCity = task.pickupCity;
        Topology.City deliveryCity = task.deliveryCity;

        /*if (enemyStatus.getPickUpCitiesPredictions().get(pickupCity) == null) {
            enemyStatus.getPickUpCitiesPredictions().put(pickupCity, new ArrayList<Double>());
        }*/


        // We take the predictions of the pickUp city
        enemyStatus.getPickUpCitiesPredictions().computeIfAbsent(pickupCity, k -> new ArrayList<Double>());
        List<Double> pickupCityPredictions = enemyStatus.getPickUpCitiesPredictions().get(pickupCity);


        // We take the predictions of the delivery city
        enemyStatus.getDeliveryCitiesPredictions().computeIfAbsent(deliveryCity, k -> new ArrayList<Double>());
        List<Double> deliveryCityPredictions = enemyStatus.getDeliveryCitiesPredictions().get(deliveryCity);

        double error = 0;

        if (enemyStatus.hasAlreadyBid()) {

            Map<Topology.City, List<Double>> pickUpCitiesBids = enemyStatus.getPickUpCitiesBids();
            Map<Topology.City, List<Double>> deliveryCityBids = enemyStatus.getDeliveryCitiesBids();

            int numberOfBids = 0;

            // If there are old offers for that pickUp City, we use their average to make our prediction
            if (pickUpCitiesBids.keySet().contains(pickupCity)) {

                List<Double> puBids = pickUpCitiesBids.get(pickupCity);

                // For every auction, we compute the abs error and then we take the average of them
                computeError(puBids, error, numberOfBids, pickupCityPredictions);

            }

            // If there are old offers for that delivery City, we use their average to make our prediction
            if (deliveryCityBids.keySet().contains(deliveryCity)) {

                List<Double> dBids = deliveryCityBids.get(deliveryCity);

                // For every auction, we compute the abs error and then we take the average of them
                computeError(dBids, error, numberOfBids, deliveryCityPredictions);
            }

            // error is the average error that we did in our predictions
            error /= numberOfBids;

            // Our final prediction will be equal to the sum of our prediction from the strategy
            // and the average of the errors on those two cities
            prediction += error;
        }

        // Adding our predictions to the enemy status
        pickupCityPredictions.add(prediction);
        deliveryCityPredictions.add(prediction);

        double agentError = enemyStatus.getAverageError();

        ArrayList<Double> range = new ArrayList<>(2);
        range.add(prediction - agentError);
        range.add(prediction + agentError);

        return range;

    }

    /**
     * This method compute the sum of the errors between the real offers and our predictions
     * @param bids
     * @param error
     * @param numberOfBids
     * @param cityPredictions
     */
    private void computeError(List<Double> bids, double error, int numberOfBids, List<Double> cityPredictions) {

        for (int i = 0; i < bids.size(); i++) {

            // It can happen that an agent makes a null offer
            if (bids.get(i) != null) {
                error += bids.get(i) - cityPredictions.get(i);
                numberOfBids++;
            }
        }
    }


    //TODO: chiamare a auction completed
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

            // If it's our agent we break
            if (enemy == agent.id()) {
                if (lastWinner == agent.id()) {
                    //TODO: bisogna aggiungere qua la task vinta?
                    //TODO: bisogna updatare qua le task importanti?
                    //TODO: update bilancio
                    balance += lastOffers[enemy];

                    updateNeededTasks();
                    //TODO: update altre mappe
                }
                continue;
            }

            if (firstAuction) {
                //TODO: controllare che nel taskset non ci sia già la task vinta
                agentStatusMap.put(enemy, new AgentStatus(agent.getTasks()));
            }

            AgentStatus enemyStatus = agentStatusMap.get(enemy);

            Map<Topology.City, List<Double>> pickUpCitiesBids = enemyStatus.getPickUpCitiesBids();
            Map<Topology.City, List<Double>> deliveryCitiesBids = enemyStatus.getDeliveryCitiesBids();

            updateOffers(pickUpCitiesBids, pickUpCity, lastOffers[enemy]);
            updateOffers(deliveryCitiesBids, deliveryCity, lastOffers[enemy]);

            // Here we compute the error we made and we add it to the errors of the status
            double error;

            // It can happen that the offer of the enemy is null - he didn't participate at the auction
            if (lastOffers[enemy] != null) {

                // We take our last prediction and we subtract his last offer
                error = Math.abs(pickUpCitiesBids.get(pickUpCity).get(pickUpCitiesBids.size() - 1) - lastOffers[enemy].doubleValue());
            }

            // In the case he didn't participate at the auction our error is equal to our prediction
            else {
                error = pickUpCitiesBids.get(pickUpCity).get(pickUpCitiesBids.size() - 1);
            }

            enemyStatus.getErrors().add(error);

            if (enemy == lastWinner) {
                enemyStatus.getTasksWon().add(lastTask);
            }
        }

        if (firstAuction) {
            firstAuction = false;
        }

    }

    /**
     * Update the offers made by the agent
     * @param previousBids
     * @param city
     * @param offer
     */
    private void updateOffers(Map<Topology.City, List<Double>> previousBids, Topology.City city, double offer) {

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

    /**
     * Method that create the vehicles for a SLS of an enemy
     * @param agentVehicles
     */
    private List<Vehicle> createVehicles(List<Vehicle> agentVehicles, int numberOfVehicles) {

        List<Vehicle> enemyVehicles = new ArrayList<>();

        for (Integer i = 0; i < numberOfVehicles; i++) {

            int randIndex = new Random().nextInt(agentVehicles.size());

            Vehicle agentVehicle = agentVehicles.get(randIndex);

            int capacity = computeCapacity(agentVehicle.capacity(), agentVehicles.size()/numberOfVehicles);

            Topology.City home = computeHome();

            Vehicle vehicle = (new VehicleImpl(i, i.toString(), capacity, agentVehicle.costPerKm(), home, (long) agentVehicle.speed(), agentVehicle.color())).getInfo();

            enemyVehicles.add(vehicle);
        }

        return enemyVehicles;
    }

    /**
     * Select a random home of a vehicle
     * @return
     */
    private Topology.City computeHome() {

        List<Topology.City> cities = topology.cities();

        int random = new Random().nextInt() * (cities.size() - 1);

        return cities.get(random);

    }

    /**
     * Compute the capacity of a new vehicle: it's between 0.8 and 1.2 the original capacity
     * @param capacity
     * @param ratio
     * @return
     */
    private int computeCapacity(int capacity, int ratio) {

        return ratio * capacity;

    }

    private double makeBid(Task task) {

        double offer = presentMarginalCost(task, agent.getTasks(), agent.vehicles(), agent.id());

        Vehicle vehicleChosen = temporaryBestSolutionMap.get(agent.id()).getVehicle(task);

        if (balance > balanceThreshold) {

            //TODO: raccattare da qualche parte il costPerKM --> RISOLTO
            double costBound = task.pathLength() * vehicleChosen.costPerKm();

            offer = futureMarginalCost(task, offer, costBound, 5);
        }


        double minBid = predictBids(task);

        // If it's one of the first auctions we are bidding very low in order to get the tasks
        if (auctionNumber < 3) {
            auctionNumber++;

            if (minBid < offer) {
                return minBid * 0.5;
            }
            else {
                return offer * 0.5;
            }
        }

        auctionNumber++;

        // TODO: rivedere se neededTask.get è negativo

        //TODO: se la task è needed offro magari 80% --> RISOLTO
        if (offer < minBid) {
            //TODO: contains sbagliato --> RISOLTO
            if (!taskSetSameCities(neededTasksMap.keySet(), task)) {
                offer = Math.max(0.95 * minBid, offer);
            }

            //TODO: controlla di avere abbastanza soldi
            else offer = Math.max(0.80 * minBid, offer);
        }

        //TODO: il contains funziona qui? No, aggiungere funzione che sfanculi il contains --> RISOLTO
        // If my initial offer is greater than minBid means that we are going to lose the auction
        // For this reason we are going to decrease our offer, but only if we really need the task
        if (offer >= minBid && taskSetSameCities(neededTasksMap.keySet(), task)) {

            Double rangeOfDecrease = neededTasksMap.get(task);

            // If my initial offer minus the range of decrease is lower than the minimum bid it means that
            // I can beat the minimum bid
            if (offer - rangeOfDecrease < minBid) {
                offer = Math.max(0.95 * minBid, offer - rangeOfDecrease);
            }

        }

        return offer;
    }

    /**
     * check if two tasks have the same pickup and delivery cities
     */
    private boolean sameCities(Task task1, Task task2){
        return task1.pickupCity.equals(task2.pickupCity) && task1.deliveryCity.equals(task2.deliveryCity);
    }

    private boolean taskSetSameCities(Set<Task> taskSet, Task task){

        for (Task task1 : taskSet) {
            if (!sameCities(task1, task))
                return false;
        }

        return true;
    }
}
