package template;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.simulation.VehicleImpl;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

/**
 * Created by Christian on 24/11/2017.
 */

public class AuctionAgentMain implements AuctionBehavior {


    private final double MIN_OFFER_COEFFICIENT = 0.20;
    private final double DEFAULT_ERROR_RATIO = 1;
    private final int INITIAL_ROUNDS_RANGE = 2;
    private Topology topology;
    private Agent agent;
    private Vehicle vehicle;
    private Random random;
    private long timeout_bid;
    private long timeout_plan;
    private Topology.City currentCity;


    ///////local fields for the strategy//////////

    private final int MAX_ITER = 5000;
    private final int SLS_THRESHOLD = 100;
    private final double SLS_PROB = 0.35;
    private double SAFETY_BOUND_REWARD = 0.1;

    private int totalAmountOfMyWonBids;
    private int auctionNumber;
    private double lastRoundWeightedErrorRatio;
    private Solution myCurrentSolution;
    private Solution myTemporarySolution;

    /////fields for the enemy agents' datas////////
    private Map<Integer, List<Double>> enemiesErrorRatiosMap;
    private Map<Integer, List<Long>> enemiesWonBidsMap;
    private Map<Integer, Set<Task>> agentsTasksWonMap;
    private Map<Integer, Double> enemiesLastPrediction;

    private Map<Integer, Double> enemiesCurrentSolutionCostMap;
    private Map<Integer, Double> enemiesTemporarySolutionCostMap;
    private Map<Integer, List<Long>> enemiesAllBidsMap;

    /////fields for the future tasks predictions////////
    private Map<Task, Double> taskProbabilityMap;
    private List<Map.Entry<Task, Double>> orderedTaskProbList;



    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.topology = topology;
        this.agent = agent;
        this.vehicle = agent.vehicles().get(0);
        this.currentCity = vehicle.homeCity();

        /// variables for strategy///
        totalAmountOfMyWonBids = 0;
        enemiesErrorRatiosMap = new HashMap<>();
        enemiesWonBidsMap = new HashMap<>();
        agentsTasksWonMap = new HashMap<>();
        enemiesLastPrediction = new HashMap<>();
        enemiesCurrentSolutionCostMap = new HashMap<>();
        enemiesTemporarySolutionCostMap = new HashMap<>();
        enemiesAllBidsMap = new HashMap<>();

        taskProbabilityMap = new HashMap<>();
        orderedTaskProbList = new ArrayList<>();

        /*
        creating al the possible combinations of tasks with the
        specific topology, only the task with probability higher than
        the probability bound argument are saved.
         */
        createTasks(topology, distribution, 0.10);


        long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
        this.random = new Random(seed);

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // the plan method cannot execute more than timeout_bid milliseconds
        timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
        System.out.println("BID:");
        System.out.println(timeout_bid);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        System.out.println("PLAN:");
        System.out.println(timeout_plan);

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

    /**
     * creating an ordered list of entries of the possible future tasks,
     * the order is ascending by the probability.
     */
    private void createOrderedTasksList() {

        orderedTaskProbList = new ArrayList<>(taskProbabilityMap.entrySet());
        Collections.sort( orderedTaskProbList, new Comparator<Map.Entry<Task, Double>>() {
            public int compare(Map.Entry<Task, Double> o1, Map.Entry<Task, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
    }

    // for description see comments inside the method
    @Override
    public Long askPrice(Task task) {
        /*
         *  DESCRIPTION:
         * computes our offer for the task in the current auction. First,
         * it evaluates our marginal cost, then it updates the offer considering
         * the minimum prediction of the bid of our enemies. After this, it controls
         * that our offer is not less then our minimum threshold and finally
         * it updates the offer if our total reward is less than the cost of
         * our current best solution for the plan. In the special case that we
         * have a higher net reward than our enemies and our next net reward will
         * be higher of our current net Reward, we update our offer considering
         * the also the probability of obtain future tasks (more details below).
         */

        Set<Task> tasks = new HashSet<>();

        // initializing the current and temporary solutions of our agent
        if(myCurrentSolution == null){

            tasks.add(task);

            myCurrentSolution = new Solution(tasks, agent.vehicles());
            myTemporarySolution = new Solution(tasks, agent.vehicles());
        }

        else {
            for (Task task1 : agent.getTasks()) {
                tasks.add(task1);
            }
            tasks.add(task);
        }

        //evaluating our next solution with also the task in the auction
        CompanyStrategy companyStrategy = new CompanyStrategy(tasks, agent.vehicles());
        myTemporarySolution = companyStrategy.SLS(MAX_ITER, timeout_bid, SLS_PROB, SLS_THRESHOLD,
                companyStrategy.initialSolution());

        // evaluating our present marginal cost
        double myOffer = myTemporarySolution.objectiveFunction() - myCurrentSolution.objectiveFunction();

        //evaluating the minimum prediction among my enemies
        double minimumEnemyPrediction = predictLowestEnemyBid(task);

        if(minimumEnemyPrediction < Double.POSITIVE_INFINITY){

            /*
             if our offer is less than the minimum prediction we can raise our offer in
             order to obtain a higher reward. We raise the offer to the max between
             our offer and the 80% of the minimum prediction bid of the enemies
             */
            if(myOffer < minimumEnemyPrediction){
                myOffer = Math.max(myOffer, 0.80*minimumEnemyPrediction);
            }
            /*
             if our offer is higher than the minimum prediction we update the
             offer to the 80% of the minimum prediction bid of the enemies
              */
            else {
                myOffer = 0.80*minimumEnemyPrediction;
            }

        }

        /*
        after this, our offer must never be lower than our standard cost of task, that is the
        full cost of task (pathLenght * vehicleCost) multiplied by a coefficient (20%)
         */
        Vehicle vehicleChosen = myTemporarySolution.getVehicle(task);
        double standardCost = task.pathLength() * vehicleChosen.costPerKm();
        myOffer = Math.max(MIN_OFFER_COEFFICIENT * standardCost, myOffer);

        //for the first auctions, we are more aggressive and we bid less because we want the tasks
        if(auctionNumber < INITIAL_ROUNDS_RANGE){
            myOffer = 0.5 * myOffer;
        }


        double currentSolutionCost = myCurrentSolution.objectiveFunction();
        double myNetReward = totalAmountOfMyWonBids - (1 + SAFETY_BOUND_REWARD) * currentSolutionCost;
        double highestEnemyNetReward = getHigestEnemyNetReward();
        /*
          Finally, we check if our net gain is balanced considering
          our net gain if we win the task and the highest net gain
          of the enemy agents.
         */
        if( auctionNumber >= INITIAL_ROUNDS_RANGE){
            myOffer = balanceNetGain(task, myOffer, myNetReward, highestEnemyNetReward, myTemporarySolution);
        }


        return (long)myOffer;
    }


    /**
     * computes our net gain considering that we win the task, after this,
     * if our next net gain is less than our current net gain it controls
     * if we are loosing (current net gain < enemy net gain), in the positive
     * case it raises the offer in order to maintain the same loss. In the
     * negative case (we are winning) it checks if our next net gain is less
     * then the enemy net gain, in that case it raises the offer in order to
     * not lose againts the enemy, in the other case it allows a loss of the
     * 5% between the current net gain and the next net gain.
     * In the special case of winning and with the next net gain higher than the
     * current net gain, we produce a new offer considering the probability
     * of find a specific task (more details in the method specification)
     */
    private double balanceNetGain(Task task, double myOffer, double myNetReward, double highestEnemyNetReward, Solution myTemporarySolution) {

        double futureNetGain = totalAmountOfMyWonBids + myOffer - (1 + SAFETY_BOUND_REWARD) * myTemporarySolution.objectiveFunction();

        double updatedOffer = myOffer;
        if(myNetReward > futureNetGain){
            // our next net gain is less than our current net gain
            if(myNetReward < highestEnemyNetReward){
                /*
                we are loosing, we update the offer in order to
                not increase our loss.
                 */
                updatedOffer += Math.abs(Math.abs(myNetReward) - Math.abs(futureNetGain));
            }
            else if (futureNetGain < highestEnemyNetReward){
                /*
                we are winning but our next net gain prevision is less than
                the enemy's net gain. We increase the offer in order to avoid this
                 */
                updatedOffer += Math.abs(Math.abs(highestEnemyNetReward) - Math.abs(futureNetGain));
            }
            else {

                /*
                we are winning even if the next net gain is less then the current
                one. We allow a allows a loss of the 5% between the current net gain
                and the next net gain.
                 */
                updatedOffer += 0.95 * Math.abs(Math.abs(myNetReward) - Math.abs(futureNetGain));
            }

        }

        /*
         * special case of winning and with the next net gain higher than the
         * current net gain, we produce a new offer considering the probability
         * of find a specific task
         */
        else if(myNetReward - highestEnemyNetReward > 0){
            //FUTURE//
            double futureMarginalCost = futureMarginalCost(task, myOffer, 3);
            Double tempOffer = Math.min(myOffer, futureMarginalCost);
            if((totalAmountOfMyWonBids + tempOffer - (1 + SAFETY_BOUND_REWARD) * myTemporarySolution.objectiveFunction())
                    > highestEnemyNetReward){
                updatedOffer = tempOffer;
            }
        }

        return updatedOffer;
    }


    /**
     * updates the present marginal cost considering future tasks
     */
    public double futureMarginalCost(Task task, double presentMarginalCost, int futureTasks){

        /*
            considers only the first 'n' task with highest probability.
         */

        Map<Task, Double> marginalCostFutureTasks = new HashMap<>();

        /*
         for each task that we have created, we evaluate the marginalCost
         of the current task in the auction, considering that we have won
         also the futureTask
         */
        for (Task futureTask : getTasksByProbability(futureTasks)) {

            Set<Task> taskSet = new HashSet<>();
            for (Task task1 : agent.getTasks()) {
                taskSet.add(task1);
            }
            taskSet.add(futureTask);

            // evaluating a solution considering the task that we won and the future task
            CompanyStrategy companyStrategy = new CompanyStrategy(taskSet, agent.vehicles());

            Solution presentTempSolution = companyStrategy.SLS(MAX_ITER, timeout_bid, SLS_PROB, SLS_THRESHOLD,
                    companyStrategy.initialSolution());

            // adding the task of the current auction to the set of tasks.
            taskSet.add(task);

            // evaluating the solution with this new set of tasks.
            companyStrategy = new CompanyStrategy(taskSet, agent.vehicles());
            Solution futureTempSolution = companyStrategy.SLS(MAX_ITER, timeout_bid, SLS_PROB, SLS_THRESHOLD,
                    companyStrategy.initialSolution());

            // here we evaluate the futureMarginalCost of the current task
            double futureMarginalCost = futureTempSolution.objectiveFunction() - presentTempSolution.objectiveFunction();

            // the future marginal costs higher than the presentMarginalCost are discarded
            if (futureMarginalCost > 0 && futureMarginalCost < presentMarginalCost) {
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
        double finalMarginalCost = presentMarginalCost;
        if(probSum > 0){
            finalMarginalCost -= weightedMarginalCost/probSum;
        }

        return finalMarginalCost;
    }

    /**
     * returns the first 'numberOfTask' tasks with highest probability
     */
    private Set<Task> getTasksByProbability(int numberOfTask){

        Set<Task> tasks = new HashSet<>();
        for (int index = 0; index < numberOfTask; index++) {
            tasks.add(orderedTaskProbList.get(index).getKey());
        }

        return tasks;
    }


    /**
     * this method evaluates the prediction for the bid of each enemy in
     * the auction. For each enemy, first it evaluates the marginal cost
     * considering the tasks won of the enemy, then it multiplies this prediction
     * for a error ratio, in order to take into account all our mistakes
     * in our previous predictions for that enemy. Finally it returns
     * the prediction with the lowest value.
     */
    private double predictLowestEnemyBid(Task task) {

        double lowestEnemyBid = Double.POSITIVE_INFINITY;

        for (Map.Entry<Integer, List<Double>> enemyErrorRatio : enemiesErrorRatiosMap.entrySet()) {

            int enemyID = enemyErrorRatio.getKey();
            List<Double> errorRatiosList = enemyErrorRatio.getValue();

            /*
              computing the error ratio in order to take into account all
              our previous error regarding the prediction
             */
            double errorRatio = computeErrorRatio(errorRatiosList);

            // computing the marginal cost for the enemy
            double enemyBid = predictEnemyBid(enemyID, task);

            // updating the marginal cost with the error ratio
            double prediction = errorRatio * enemyBid;

            if(prediction < lowestEnemyBid){
                lowestEnemyBid = prediction;
            }

            /* saving our last prediction for the specific enemy,
               useful for the update of the error list when the
               auction is ended.
             */
            enemiesLastPrediction.put(enemyID, prediction);

        }

        return lowestEnemyBid;
    }

    /**
     * computing the weighted error ratio between predictions and
     * real bids, the errors of the last auctions weight more.
     */
    private double computeErrorRatio(List<Double> errorRatiosList) {

        double weightedError = 0.;
        double weights = 0;

        /*
         this is our coefficient for taking into account the importance
         of the past.
         */
        double gammaFuture = 0.8;

        for (int index = errorRatiosList.size() -1 ; index >= 0 ; index--) {


            weightedError += gammaFuture*errorRatiosList.get(index);
            weights += gammaFuture;

        /*
          the coeffiecient is squared in every iteration in order
           to give less weight to the errors of the past. This is
           because we want to react fast to a fast change of our
           enemy strategy.
         */
            gammaFuture = Math.pow(gammaFuture, 2);
        }

        double currentRoundErrorRatio = weightedError / (weights + 1e-10);

        //bounding the error rapport
        currentRoundErrorRatio = Math.min(3, currentRoundErrorRatio);
        currentRoundErrorRatio = Math.max(0.01, currentRoundErrorRatio);

        return currentRoundErrorRatio;
    }

    /**
     * Method that create the vehicles for a SLS of an enemy.
     * it computes the capacity and a random home city.
     */
    private List<Vehicle> createVehicles(List<Vehicle> agentVehicles, int numberOfVehicles) {

        List<Vehicle> enemyVehicles = new ArrayList<>();

        for (Integer i = 0; i < numberOfVehicles; i++) {

            int randIndex = new Random().nextInt(agentVehicles.size());

            Vehicle agentVehicle = agentVehicles.get(randIndex);

            TaskSet emptyTaskSet = agentVehicle.getCurrentTasks();
            emptyTaskSet.removeAll(emptyTaskSet);

            Double ratio = (double)agentVehicles.size() / (double)numberOfVehicles;

            int capacity = computeCapacity(agentVehicle.capacity(), ratio);

            Topology.City home = computeHome();

            VehicleImpl vehicleImpl = new VehicleImpl(i, i.toString(), capacity, agentVehicle.costPerKm(), home,
                    (long) agentVehicle.speed(), agentVehicle.color());

            vehicleImpl.setTasks(emptyTaskSet);

            enemyVehicles.add(vehicleImpl.getInfo());
        }

        return enemyVehicles;
    }

    /**
     * Select a random home of a vehicle.
     * @return
     */
    private Topology.City computeHome() {

        List<Topology.City> cities = topology.cities();
        int random = new Random().nextInt(cities.size() - 1);
        return cities.get(random);
    }

    /**
     * Compute the capacity of a new vehicle: it's the ratio between
     * the number of vehicles of our agent and the number of vehicles
     * of the new agent.
     */
    private int computeCapacity(int capacity, double ratio) {

        return (int)(ratio * capacity);

    }

    /**
     * it computes the marginal cost for our enemy. During this
     * prediction the number of vehicles of the enemy agent are not know
     * so it computes four different prediction with 2, 3, 4 and 5 vehicles
     * and then it evaluates the mean of the prediction. After this,
     * marginal cost is obtained by subtracting the current solution cost
     * for the enemy.
     */
    private double predictEnemyBid(int enemyID, Task task) {

        // creating the different list of vehicles:
        List<Vehicle> vehicles2 = createVehicles(agent.vehicles(), 2);
        List<Vehicle> vehicles3 = createVehicles(agent.vehicles(), 3);
        List<Vehicle> vehicles4 = createVehicles(agent.vehicles(), 4);
        List<Vehicle> vehicles5 = createVehicles(agent.vehicles(), 5);

        // creating a set of task with the tasks won by the enemy and the task of the auction:
        Set<Task> taskWon = new HashSet<>();
        if (agentsTasksWonMap.keySet().contains(enemyID)) {
            for (Task task1 : agentsTasksWonMap.get(enemyID)) {
                taskWon.add(task1);
            }
        }
        taskWon.add(task);

        // evaluating the different predictions using the lists of vehicles:
        CompanyStrategy companyStrategy = new CompanyStrategy(taskWon, vehicles2);
        Solution solution2 = companyStrategy.SLS(MAX_ITER, timeout_bid, SLS_PROB,
                SLS_THRESHOLD, companyStrategy.initialSolution());

        double prediction2 = solution2.objectiveFunction();
        Vehicle vehicleChosen2 = solution2.getVehicle(task);

        companyStrategy = new CompanyStrategy(taskWon, vehicles3);
        Solution solution3 = companyStrategy.SLS(MAX_ITER, timeout_bid, SLS_PROB,
                SLS_THRESHOLD, companyStrategy.initialSolution());

        double prediction3 = solution3.objectiveFunction();
        Vehicle vehicleChosen3 = solution3.getVehicle(task);

        companyStrategy = new CompanyStrategy(taskWon, vehicles4);
        Solution solution4 = companyStrategy.SLS(MAX_ITER, timeout_bid, SLS_PROB,
                SLS_THRESHOLD, companyStrategy.initialSolution());

        double prediction4 = solution4.objectiveFunction();
        Vehicle vehicleChosen4 = solution4.getVehicle(task);

        companyStrategy = new CompanyStrategy(taskWon, vehicles5);
        Solution solution5 = companyStrategy.SLS(MAX_ITER, timeout_bid, SLS_PROB,
                SLS_THRESHOLD, companyStrategy.initialSolution());

        double prediction5 = solution5.objectiveFunction();
        Vehicle vehicleChosen5 = solution5.getVehicle(task);


        // evaluating the marginal cost: calculating the mean of prediction and the subtracting the enemy cost
        double meanCostPrediction = (prediction2 + prediction3 + prediction4 + prediction5)/4.0;

        // saving our cost prediction in the temporary map (useful at the end of each auction)
        enemiesTemporarySolutionCostMap.put(enemyID, meanCostPrediction);

        Double currentEnemyCost = enemiesCurrentSolutionCostMap.get(enemyID);
        if(currentEnemyCost == null){
            currentEnemyCost = 0.;
        }
        double prediction = meanCostPrediction - currentEnemyCost;

        /*
         if our prediction is less than zero we use the mean value
         of all the enemy's real bids.
         */
        if(prediction < 0){
            prediction = getEnemyMeanRealBids(enemyID);
        }


        double standardCost = task.pathLength()*((double)(vehicleChosen2.costPerKm() + vehicleChosen3.costPerKm() +
                vehicleChosen4.costPerKm() + vehicleChosen5.costPerKm()) / 4.0);
        if(prediction < standardCost){
            prediction = Math.max(standardCost * MIN_OFFER_COEFFICIENT, prediction);
        }


        /*
         Finally, it checks if the ratio between our prediction and the mean
         of the real bids is out of a range computed with the standard deviation
         of the real bids. In that case it bounds the prediction.
         */
        double meanEnemyRealBids = getEnemyMeanRealBids(enemyID);
        double stdEnemyRealBids = getEnemyStdRealBids(enemyID);

        if(meanEnemyRealBids > 0
                && (prediction / meanEnemyRealBids > (1.0 + 0.5*(stdEnemyRealBids/meanEnemyRealBids))
                || prediction / meanEnemyRealBids < 1.0 - 0.5*(stdEnemyRealBids/meanEnemyRealBids)) ){
            double maxBound = (1 + 0.50) * meanEnemyRealBids;
            double minBound = (1 - 0.50) * meanEnemyRealBids;
            prediction = Math.min(maxBound, prediction);
            prediction = Math.max(minBound, prediction);
        }

        return prediction;

    }

    private double getEnemyStdRealBids(int enemyID) {

        double mean = getEnemyMeanRealBids(enemyID);
        List<Long> enemyBids = enemiesAllBidsMap.get(enemyID);
        double sum = 0.;

        for (Long enemyBid : enemyBids) {
            sum += Math.pow(enemyBid - mean, 2);
        }

        double variance = sum / enemiesAllBidsMap.size();
        return Math.sqrt(variance);
    }

    private double getEnemyMeanRealBids(int enemyID) {

        List<Long> enemyRealBids = enemiesAllBidsMap.get(enemyID);
        double sum = 0.;
        for (Long enemyRealBid : enemyRealBids) {
            sum += enemyRealBid.doubleValue();
        }
        return  sum / enemyRealBids.size();
    }

    /**
     * returns the sum of the bid for tasks won
     * of the enemy agent.
     */
    private double getEnemyTotalReward(int enemyID) {

        double cost = 0.;
        if(enemiesWonBidsMap.keySet().contains(enemyID)){
            for (Long enemyRealBid : enemiesWonBidsMap.get(enemyID)) {
                cost += enemyRealBid;
            }
        }
        return cost;
    }

    // for the description see comments inside the code
    @Override
    public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {

        /*
         it saves the task won in the set of task won for the winner agent
         in the agentsTasksWonMap (useful for the evaluation of the marginal cost)
         */
        if(!agentsTasksWonMap.keySet().contains(lastWinner)){
            agentsTasksWonMap.put(lastWinner, new HashSet<>());
        }
        agentsTasksWonMap.get(lastWinner).add(lastTask);


        for (int enemyID = 0; enemyID < lastOffers.length; enemyID++) {

            if(enemyID != agent.id()){

                /*
                  for the winner enemy agent it saves the bid in the list
                  inside the 'enemiesWonBidsMap' and we update its current cost
                  solution.
                 */
                Long lastOffer = lastOffers[enemyID];
                if(lastWinner == enemyID){
                    if(!enemiesWonBidsMap.keySet().contains(enemyID)){
                        enemiesWonBidsMap.put(enemyID, new ArrayList<>());
                    }
                    enemiesWonBidsMap.get(enemyID).add(lastOffer);
                    Double tempEnemyCost = enemiesTemporarySolutionCostMap.get(enemyID);
                    if(tempEnemyCost == null){
                        tempEnemyCost = myTemporarySolution.objectiveFunction();
                    }
                    enemiesCurrentSolutionCostMap.put(enemyID, tempEnemyCost);
                }

                // for each enemy it saves its bid in the enemiesAllBidsMap.
                if(lastOffer != null){
                    if(!enemiesAllBidsMap.keySet().contains(enemyID)){
                        enemiesAllBidsMap.put(enemyID, new ArrayList<>());
                    }
                    enemiesAllBidsMap.get(enemyID).add(lastOffer);
                }

                /*
                    it evaluates the last error ratio between the actual bid
                    of the enemy and our prediction of the bid of the enemy.
                 */
                Double lastPrediction = enemiesLastPrediction.get(enemyID);

                Double newErrorRatio;
                if(lastOffer == null) {
                    newErrorRatio = DEFAULT_ERROR_RATIO;
                    if(lastPrediction == null){
                        enemiesLastPrediction.put(enemyID, 0.);
                    }
                }
                else {
                    newErrorRatio = lastOffer.doubleValue();
                    if(lastPrediction != null){
                        newErrorRatio = newErrorRatio / lastPrediction;
                    }
                    else {
                        lastRoundWeightedErrorRatio = DEFAULT_ERROR_RATIO;
                        newErrorRatio = DEFAULT_ERROR_RATIO;
                        enemiesLastPrediction.put(enemyID, lastOffer.doubleValue());
                    }
                }

                // it adds the new error ratio in the list of error in the 'enemiesErrorRatiosMap'.
                if(!enemiesErrorRatiosMap.keySet().contains(enemyID)){
                    enemiesErrorRatiosMap.put(enemyID, new ArrayList<>());
                }
                enemiesErrorRatiosMap.get(enemyID).add(newErrorRatio);
            }

        }

        /*
          if our agent won the auction it updates the variables of our agent,
          the 'totalAmountOfMyWonBids' and the 'myCurrentSolution'.
         */
        if(lastWinner == agent.id()){
            totalAmountOfMyWonBids += lastOffers[lastWinner];
            myCurrentSolution = new Solution(myTemporarySolution);
        }

        auctionNumber++;

    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        /**
         * it uses the SLS method in order to obtain a best solution for all the task
         */

        if (tasks.isEmpty()) {
            return createEmptyPlan(vehicles);
        } else {

            CompanyStrategy companyStrategy = new CompanyStrategy(tasks, vehicles);
            System.out.println("starting SLS");
            long start = System.currentTimeMillis();
            Solution bestSolution = companyStrategy.SLS(10000, timeout_bid, 0.35, 200,
                    companyStrategy.initialSolution());
            long end = System.currentTimeMillis();
            System.out.println("completed SLS in " + (end - start) / 1000 + "seconds");

            List<Plan> planList = new ArrayList<>();

            for (Vehicle vehicle : vehicles) {
                planList.add(createVehiclePlan(vehicle, bestSolution.getVehicleActionMap().get(vehicle)));
            }

            //only for print results//////
            double bestSolutionCost = bestSolution.objectiveFunction();
            double reward = 0.;
            for (Task task : agent.getTasks()) {
                reward += task.reward;
            }

            System.out.println("my net GAIN: " + (totalAmountOfMyWonBids - bestSolutionCost));

            System.out.println("AGENT " + agent.id() + ": BID REWARD = " + reward +
                    " FINAL SOLUTION COST = " + bestSolutionCost + "CURRENT SOLUTION COST: " +
                    myCurrentSolution.objectiveFunction());
            ////////////////

            return planList;
        }
    }

    private List<Plan> createEmptyPlan(List<Vehicle> vehicles) {

        List<Plan> planList = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            planList.add(new Plan(vehicle.homeCity()));
        }
        return planList;
    }

    /**
     * it creates the plan for the vehicle by emulating its path using the actionList
     * of the vehicle.
     */
    private Plan createVehiclePlan(Vehicle vehicle, List<Action> actionList) {

        Plan plan = new Plan(vehicle.getCurrentCity());

        Topology.City currentCity = vehicle.getCurrentCity();

        for (Action action : actionList) {

            Task task = action.getTask();

            switch (action.getActionType()) {
                case PICKUP:
                    if (action.getTask().pickupCity.equals(currentCity)) {
                        plan.appendPickup(task);
                    } else {
                        for (Topology.City city : currentCity.pathTo(task.pickupCity)) {
                            plan.appendMove(city);
                        }
                        plan.appendPickup(task);
                        currentCity = task.pickupCity;
                    }
                    break;
                case DELIVERY:
                    if (action.getTask().deliveryCity.equals(currentCity)) {
                        plan.appendDelivery(task);
                    } else {
                        for (Topology.City city : currentCity.pathTo(task.deliveryCity)) {
                            plan.appendMove(city);
                        }
                        plan.appendDelivery(task);
                        currentCity = task.deliveryCity;
                    }
                    break;
                default:
                    break;
            }
        }
        return plan;
    }

    /**
     * evaluates the lowest net reward among the enemy agents
     */
    public double getHigestEnemyNetReward() {

        Double highestEnemyReward = Double.NEGATIVE_INFINITY;

        for (Map.Entry<Integer, Double> enemyCostEntry : enemiesCurrentSolutionCostMap.entrySet()) {
            int enemyID = enemyCostEntry.getKey();
            Double enemyCost = enemyCostEntry.getValue();

            Double enemyReward = getEnemyTotalReward(enemyID);

            double netReward = enemyReward - (1-SAFETY_BOUND_REWARD) * enemyCost;

            if(netReward > highestEnemyReward){
                highestEnemyReward = netReward;

            }


        }

        if(highestEnemyReward == Double.NEGATIVE_INFINITY){
            highestEnemyReward = 0.;
        }
        return highestEnemyReward;
    }
}
