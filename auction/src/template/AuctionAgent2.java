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

public class AuctionAgent2 implements AuctionBehavior {


    private final double MIN_OFFER_COEFFICIENT = 0.33;
    private final double MAX_PENALTY_ERROR = 1;
    private final int INITIAL_ROUNDS_RANGE = 3;
    private Topology topology;
    private TaskDistribution distribution;
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
    private Map<Integer, List<Double>> enemiesErrorRatiosMap;
    private Map<Integer, List<Long>> enemiesWonBidsMap;
    private Map<Integer, Set<Task>> agentsTasksWonMap;
    private Map<Integer, Double> enemiesLastPrediction;
    private Solution myCurrentSolution;
    private Solution myTemporarySolution;

    private Map<Integer, Double> enemiesCurrentSolutionCostMap;
    private Map<Integer, Double> enemiesTemporarySolutionCostMap;
    private Map<Integer, Long> enemiesLowestBidMap;
    private Map<Integer, Long> enemiesHighestBidMap;
    private Map<Integer, List<Long>> enemiesAllBidsMap;



    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.topology = topology;
        this.distribution = distribution;
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
        enemiesLowestBidMap = new HashMap<>();
        enemiesHighestBidMap = new HashMap<>();
        enemiesAllBidsMap = new HashMap<>();



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
         * our current best solution for the plan.
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

        //evaluating our future solution with also the task in the auction
        CompanyStrategy companyStrategy = new CompanyStrategy(tasks, agent.vehicles());
        myTemporarySolution = companyStrategy.SLS(MAX_ITER, timeout_bid, SLS_PROB, SLS_THRESHOLD,
                                                  companyStrategy.initialSolution());

        // our present marginal cost
        double myOffer = myTemporarySolution.objectiveFunction() - myCurrentSolution.objectiveFunction();
        System.out.println("PRESENT OFFER: " + myOffer);

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

        System.out.println("ENEMY PREDICTION " + minimumEnemyPrediction + " UPDATED OFFER: " + myOffer);


        /*
        after this, our offer is never lower than our standard cost of task, that is the
        full cost of task (pathLenght * vehicleCost) multiplied by a coefficient (33%)
         */
        Vehicle vehicleChosen = myTemporarySolution.getVehicle(task);
        double standardCost = task.pathLength() * vehicleChosen.costPerKm();
        myOffer = Math.max(MIN_OFFER_COEFFICIENT * standardCost, myOffer);
        System.out.println("OFFER AFTER STANDARD COST: " + myOffer);

        /*
          Finally, we check if our total reward ( sum of bids for tasks that we won)
          is less than our cost of current solution of the computed plan. If this is
          the case, we raise a little bit our offer because we have negative balance
         */
        double currentSolutionCost = myCurrentSolution.objectiveFunction();
        double myNetReward = totalAmountOfMyWonBids - (1 - SAFETY_BOUND_REWARD) * currentSolutionCost;
        double highestEnemyNetReward = getHigestEnemyNetReward();
        if(myNetReward - highestEnemyNetReward < 0){

            myOffer = (1 + SAFETY_BOUND_REWARD) * myOffer;
            System.out.println("OFFER AFTER SAFETY BOUND REWARD: " + myOffer);
        }

        //for the first auctions, we are more aggressive and we bid less because we want the tasks
        if(auctionNumber < INITIAL_ROUNDS_RANGE){
            myOffer = 0.66 * myOffer;
        }

        return (long)myOffer;
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

            System.out.println("STANDARD ENEMY PREDICTION: " + enemyBid + " ERROR RATIO: " + errorRatio +
                                " ERROR UPDATED PREDICTION: " + prediction);

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

        double errorRatio = weightedError / (weights + 1e-10);

        //TODO: rimuovi debug:
        if(Math.abs(errorRatio) > 10){

            System.out.println("WEIGTHED ERROR: " + weightedError);
            System.out.println("WEIGHTS : " + weights);
        }

        System.out.println("ERROR RATIO UNBOUNDED: " + errorRatio);

        //bounding the error rapport
        errorRatio = Math.min(3, errorRatio);
        errorRatio = Math.max(0.01, errorRatio);

        return errorRatio;
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
     * marginal cost is obtained by subtracting the total cost of the tasks
     * won by the enemy.
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

        // taking the total cost for the enemy of its tasks won
        //double enemyTotalCost = getEnemyTotalReward(enemyID);

        // evaluating the marginal cost: calculating the mean of prediction and the subtracting the enemy cost
        double meanCostPrediction = (prediction2 + prediction3 + prediction4 + prediction5)/4.0;
        enemiesTemporarySolutionCostMap.put(enemyID, meanCostPrediction);
        Double currentEnemyCost = enemiesCurrentSolutionCostMap.get(enemyID);
        if(currentEnemyCost == null){
            currentEnemyCost = 0.;
        }
        double prediction = meanCostPrediction - currentEnemyCost;

        System.out.println("PREDICTIONS ENEMY: 2)" + prediction2 + " 3)" + prediction3 + " 4)"
                            + prediction4 + " 5)" + prediction5);

        System.out.println("ENEMY CURRENT COST: " + currentEnemyCost);

        if(prediction < 0){
            prediction = getEnemyMeanRealBids(enemyID);
            System.out.println("NEGATIVE PREDICTION UPDATED: " + prediction);
        }

        double standardCost = task.pathLength()*((double)(vehicleChosen2.costPerKm() + vehicleChosen3.costPerKm() +
                                                    vehicleChosen4.costPerKm() + vehicleChosen5.costPerKm()) / 4.0);
        if(prediction < standardCost){
            System.out.println("PREDICTION BEFORE STD: " + prediction);
            prediction = Math.max(standardCost * 0.5, prediction);
            System.out.println(" PREDICTION STANDARD COST: " + standardCost + "UPDATE " + prediction);

        }


        double meanEnemyRealBids = getEnemyMeanRealBids(enemyID);
        double stdEnemyRealBids = getEnemyStdRealBids(enemyID);

        System.out.println("ENEMY MEAN BIDS: " + meanEnemyRealBids + " ENEMY STD: " + stdEnemyRealBids);

        if((int)meanEnemyRealBids != 0
                && prediction / meanEnemyRealBids > (1.0 + 0.5*(stdEnemyRealBids/meanEnemyRealBids))){
            System.out.println("PREDICTION UNBOUNDED: " + prediction);
            double maxBound = (1 + 0.50) * meanEnemyRealBids;
            double minBound = (1 - 0.50) * meanEnemyRealBids;
            prediction = Math.min(maxBound, prediction);
            prediction = Math.max(minBound, prediction);
        }

        //TODO: bound su media e standard deviation

        /*
         our final prediction is bounded between the
         lowest and highest bid of our enemy.

        Double lowestBid = enemiesLowestBidMap.get(enemyID).doubleValue();
        Double highestBid = enemiesHighestBidMap.get(enemyID).doubleValue();

        System.out.println("PREDICTION UNBOUNDED: " + prediction);

        System.out.println("LOWEST BID: " + lowestBid + " HIGHEST BID: " + highestBid );

        prediction = Math.min(highestBid, prediction);
        prediction = Math.max(lowestBid, prediction);*/

        System.out.println("FINAL PREDICTION RETURNED: " + prediction);

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

        System.out.println("AUCTION NUMBER: " + auctionNumber + ". THE WINNER IS AGENT " +
                           lastWinner + " WITH OFFER: " + lastOffers[lastWinner]);

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
                  for each enemy agent we save its actual bid in the list
                  inside the 'enemiesWonBidsMap'.
                 */
                Long lastOffer = lastOffers[enemyID];
                if(lastWinner == enemyID){
                    if(!enemiesWonBidsMap.keySet().contains(enemyID)){
                        enemiesWonBidsMap.put(enemyID, new ArrayList<>());
                    }
                    enemiesWonBidsMap.get(enemyID).add(lastOffer);
                    Double tempEnemyCost = enemiesTemporarySolutionCostMap.get(enemyID);
                    if(tempEnemyCost == null){
                        tempEnemyCost = lastOffer.doubleValue();
                    }
                    enemiesCurrentSolutionCostMap.put(enemyID, tempEnemyCost);
                }

                // updating the lowest and the highest bid of our enemies
                Long enemyLowestBid = enemiesLowestBidMap.get(enemyID);
                Long enemyHighestBid = enemiesHighestBidMap.get(enemyID);

                if(lastOffer != null){
                    if(!enemiesAllBidsMap.keySet().contains(enemyID)){
                        enemiesAllBidsMap.put(enemyID, new ArrayList<>());
                    }
                    enemiesAllBidsMap.get(enemyID).add(lastOffer);
                }

                if(enemyLowestBid == null || lastOffer < enemyLowestBid)
                    enemiesLowestBidMap.put(enemyID, lastOffer);

                if(enemyHighestBid == null || lastOffer > enemyHighestBid)
                    enemiesHighestBidMap.put(enemyID, lastOffer);

                /*
                    it evaluates the last error ratio between the actual bid
                    of the enemy and our prediction of the bid of the enemy.
                 */
                Double lastPrediction = enemiesLastPrediction.get(enemyID);

                Double newErrorRatio;
                if(lastOffer == null) {
                    newErrorRatio = MAX_PENALTY_ERROR;
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
                        newErrorRatio = MAX_PENALTY_ERROR;
                        enemiesLastPrediction.put(enemyID, lastOffer.doubleValue());
                    }
                }

                System.out.println("AGENT " + enemyID + " LAST ERROR RATIO: " + newErrorRatio);

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
            System.out.println("MY TOTAL REWARD: " + totalAmountOfMyWonBids);
        }

        auctionNumber++;

        // only for printing results
        for (int agentID = 0; agentID < lastOffers.length; agentID++) {
            if(agentID == agent.id()){
                System.out.println("MY BID: " + lastOffers[agentID]);
            }
            else
                System.out.println("AGENT " + agentID + " BID: " + lastOffers[agentID] + "\n\n");

        }

    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
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

            double bestSolutionCost = bestSolution.objectiveFunction();
            double reward = 0.;
            for (Task task : agent.getTasks()) {
                reward += task.reward;
            }

            System.out.println("my total reward BIDS: " + totalAmountOfMyWonBids);

            System.out.println("AGENT " + agent.id() + ": BID REWARD = " + reward +
                                " FINAL SOLUTION COST = " + bestSolutionCost + "CURRENT SOLUTION COST: " +
                                myCurrentSolution.objectiveFunction());

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

            double netReward = enemyReward - enemyCost;

            if(netReward > highestEnemyReward)
                highestEnemyReward = netReward;

        }

        return highestEnemyReward;
    }
}
