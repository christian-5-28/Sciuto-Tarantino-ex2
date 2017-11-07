package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import java.util.*;

/**
 * CompanyStrategy class:
 * this class models the behaviour for finding a centralized solution
 * with multiple vehicles for the PDP as a COP (Constraint Optimization Problem).
 * The SLS algorithm is implemented here with all the its supporting methods.
 * More details in the descriptions of each method.
 */
public class CompanyStrategy {

    private TaskSet tasksDomain;
    private List<Vehicle> vehiclesDomain;
    private int equalCostCounter = 0;


    public CompanyStrategy(TaskSet tasksDomain, List<Vehicle> vehiclesDomain) {
        this.tasksDomain = tasksDomain;
        this.vehiclesDomain = vehiclesDomain;
    }

    /**
     * Stochastic Local Search algorithm. Starting from an initial solution,
     * in each iteration it creates list of neighbors Solutions of the current Solution,
     * then it finds the solution with the minimum cost among the neighbor solutions
     * and with a probability p it selects this new solution, otherwise it keeps with
     * the current solution (more details in the description of the "localChoice" method).
     * In each steps the global best solution (minimum cost found in this search)
     * is updated if the new solution chosen has a lower cost.
     * @param maxIter
     * @param probability
     * @param minimumThreshold
     * @return
     */
    public Solution SLS(int maxIter, double probability, int minimumThreshold) {

        //initializes a first solution
        Solution solution = initialSolution();

        Solution bestSolution = solution;

        for (int i = 0; i < maxIter; i++) {

            System.out.println("iteration: " + i);
            Solution oldSolution = new Solution(solution);

            //creates the neighbors solutions
            List<Solution> neighbors = chooseNeighbors(oldSolution);

            //selects a new solution
            solution = localChoice(neighbors, probability, oldSolution, minimaThreshold);
            System.out.println("solution cost: " + solution.objectiveFunction());

            //check for the update of the global best solution
            if(solution.objectiveFunction() < bestSolution.objectiveFunction())
                bestSolution = solution;

        }

        System.out.println("BEST SOLUTION COST: " + bestSolution.objectiveFunction());
        return bestSolution;

    }


    /**
     * this method creates a first Solution where all the tasks with pickupCity the same
     * homecity of a vehicle are assigned to that vehicle. Then, if there are task not assigned,
     * they are splitted equally to each vehicle (always considering the weight of the task)
     * @return
     */
    private Solution initialSolution(){

        Solution solution = new Solution(tasksDomain, vehiclesDomain);

        for (Vehicle vehicle : vehiclesDomain) {
            solution.getVehicleActionMap().put(vehicle, new ArrayList<>());
        }


        ArrayList<Vehicle> vehicles = new ArrayList<>(vehiclesDomain);
        ArrayList<Task> tasksToAdd = new ArrayList<>(tasksDomain);

        /*
            for each vehicle we control if there are task in its homeCity, if so,
            we add pickup Action to the actionList of the vehicle. after all the tasks
            are taken for that vehicle, we add the delivery action to the actionList
            (thanks to the taskToDeliver list).
         */
        for (Vehicle vehicle : vehicles) {

            List<Task> taskToDeliver = new ArrayList<>();
            for (Task task : tasksDomain) {
                if(task.pickupCity.equals(vehicle.homeCity()) && task.weight <= vehicle.capacity() - vehicle.getCurrentTasks().weightSum()){

                    solution.getVehicleActionMap().get(vehicle).add(new Action(Action.ActionType.PICKUP, task));
                    taskToDeliver.add(task);
                    tasksToAdd.remove(task);
                }
            }

            for (Task task : taskToDeliver) {
                solution.getVehicleActionMap().get(vehicle).add((new Action(Action.ActionType.DELIVERY, task)));
                int indexOfPickup = taskToDeliver.indexOf(task);

                solution.getTaskActionTimesMap().put(task, new ActionTimes(indexOfPickup, indexOfPickup + taskToDeliver.size()));

            }

        }

        int numberOfVehicles = vehicles.size();
        int vehicleIndex = 0;

        int taskIndex = 0;
        int taskToAddNumber = tasksToAdd.size();
        /*
            if there are still task to be taken they are assigned to each vehicle equally,
            considering the weight of the task. We add the pickUp and delivery action
            to the actionList of the vehicle consequentially.
         */
        while (!tasksToAdd.isEmpty()) {

            Task task = tasksToAdd.get(taskIndex);
            Vehicle vehicle = vehicles.get(vehicleIndex);
            vehicleIndex = (vehicleIndex + 1 ) % numberOfVehicles;

            if(task.weight <= vehicle.capacity() - vehicle.getCurrentTasks().weightSum()){

                List<Action> actions = solution.getVehicleActionMap().get(vehicle);

                Action pickUp = new Action(Action.ActionType.PICKUP, task);
                Action delivery = new Action(Action.ActionType.DELIVERY, task);
                actions.add(pickUp);
                actions.add(delivery);
                solution.getTaskActionTimesMap().put(task, new ActionTimes(actions.indexOf(pickUp), actions.indexOf(delivery)));

                tasksToAdd.remove(task);
                taskToAddNumber = tasksToAdd.size();

                if(taskToAddNumber != 0)
                    taskIndex = (taskIndex + 1) % taskToAddNumber;

            }
        }

        return solution;

    }

    /**
     *
     * This method creates new Solution to be explored in the search of the SLS algorithm.
     * First, new Solution are created by changing one random task from a random vehicle to
     * all the other vehicle ( we obtain then a new solution for each vehicle different from
     * the one selected. all the new Solution are added to a neighbor solution list.
     * Second, for the random vehicle selected, we change to order of two actions of its
     * actionList, we do this pair-wise for each couple of actions. A new solution is made
     * for each action changed. we add the new solution to the neighbor list.
     * Finally, the neighbor list is filtered, it means that every solution is checked for
     * validation about the constraint that it has.
     *
     */
    public List<Solution> chooseNeighbors(Solution oldSolution) {

        ArrayList<Solution> neighbors = new ArrayList<>();

        // Getting random vehicle from the vehicle domain
        List<Vehicle> vehiclesWithTask = new ArrayList<>();
        for (Vehicle vehicle : vehiclesDomain) {
            if(!oldSolution.getVehicleActionMap().get(vehicle).isEmpty()){
                vehiclesWithTask.add(vehicle);
            }
        }
        int randVehicleIndex = new Random().nextInt(vehiclesWithTask.size());
        Vehicle randVehicle = vehiclesWithTask.get(randVehicleIndex);

        // Getting random task from the task domain of the vehicle
        List<Action> vehicleActions = oldSolution.getVehicleActionMap().get(randVehicle);
        int randTaskIndex = new Random().nextInt(vehicleActions.size());
        Task taskToSwitch = oldSolution.getVehicleActionMap().get(randVehicle).get(randTaskIndex).getTask();

        /*
        For every vehicle in the domain, if the vehicle is != randVehicle, then
        we switch the random task between the two vehicles and we create
        a new candidate solution
        */

        System.out.println("starting switch task");
        for (Vehicle vehicle : oldSolution.getVehiclesDomain()) {

            // TODO: vedere se aggiungere come condizione dell'if il controllo della free capacity del vehicle
            // TODO: a cui viene aggiunta la task rispetto alla task che gli stiamo dando
            if(vehicle.id() != randVehicle.id()){
                neighbors.add(changingVehicle(oldSolution, randVehicle, vehicle, taskToSwitch));
            }
        }


        /*
            If the current vehicle has more than one task, we change the order of two actions
            in the actionsList of the vehicle. We do this for pair-wise action element
            and for each change we obtain a new local Solution
         */
        System.out.println("starting permutation");

        List<Action> vehicleActionsList = oldSolution.getVehicleActionMap().get(randVehicle);
        int vehicleActionsListSize = vehicleActionsList.size();

        if(vehicleActionsListSize > 2){
            for(int firstIndex = 0; firstIndex < vehicleActionsListSize - 1; firstIndex++){
                for(int secondIndex = firstIndex + 1; secondIndex < vehicleActionsListSize; secondIndex++){
                    neighbors.add(swapActions(oldSolution, randVehicle, vehicleActionsList, firstIndex, secondIndex));
                }
            }
        }

        /*
            Before returning all the neighbors solutions founded, we filter the new solution in order to
            obtain only valid new solutions.
         */
        neighbors = filterOnConstraints(neighbors);

        return neighbors;

    }

    /**
     * it changes the order of two actions in the actionList of the selected vehicle.
     * The two actions are selected by index. After the change, the updateTime Method
     * modifies the taskActionTimes map of the solution due to the order change in the
     * actionList.
     */
    private Solution swapActions(Solution oldSolution, Vehicle vehicle, List<Action> vehicleActionsList, int firstIndex, int secondIndex) {

        Solution newSolution = new Solution(oldSolution);

        //we select the two actions
        Action firstAction = vehicleActionsList.get(firstIndex);
        Action secondAction = vehicleActionsList.get(secondIndex);

        //we change the order of the two actions in the vehicleAction list
        newSolution.getVehicleActionMap().get(vehicle).set(firstIndex, secondAction);
        newSolution.getVehicleActionMap().get(vehicle).set(secondIndex, firstAction);

        //due to the order change, we update the taskActionTimes map in the solution.
        updateTimes(newSolution, vehicle);

        return newSolution;

    }

    /**
     * removes the selected task from the vehicle 1 and adds it in the vehicle 2. In order to make more
     * simple the way to get and remove the actions regarding the selected task, this method uses the taskActiontimes
     * map that, for each task has a actionTime values that contains the pickUpTime value of the task
     * (the index of the pickup action of that task in the actionList) and the deliveryTime of the task.
     * It adds the two actions randomly in the second vehicle (respecting the order of pickup and delivery), then
     * the updateTime method is called on the new solution and on the two vehicles.
     */
    public Solution changingVehicle(Solution oldSolution, Vehicle v1, Vehicle v2, Task taskToSwitch) {

        Solution tempSolution = new Solution(oldSolution);

        // We get the actionTimes (indexes of pickup and delivery of the task in the vehicle action list)
        ActionTimes actionTimes = tempSolution.getTaskActionTimesMap().get(taskToSwitch);

        // We remove from the vehicle (v1) the actions that it is not going to do anymore
        Action pickUp = tempSolution.getVehicleActionMap().get(v1).get(actionTimes.pickUpTime);
        Action delivery = tempSolution.getVehicleActionMap().get(v1).get(actionTimes.deliveryTime);
        tempSolution.getVehicleActionMap().get(v1).remove(actionTimes.pickUpTime);
        tempSolution.getVehicleActionMap().get(v1).remove(actionTimes.deliveryTime - 1);


        // We add those two actions to the vehicle v2 in a random position
        List<Action> v2Actions = tempSolution.getVehicleActionMap().get(v2);
        int pickUpIndex = new Random().nextInt(v2Actions.size() + 1);

        if (pickUpIndex >= v2Actions.size()) {
            v2Actions.add(pickUp);
            v2Actions.add(delivery);
        }

        else {

            v2Actions.add(pickUpIndex, pickUp);

            int deliveryIndex = new Random().nextInt(v2Actions.size() + 1);
            while (deliveryIndex <= pickUpIndex) {
                deliveryIndex = new Random().nextInt(v2Actions.size() + 1);
            }

            if (deliveryIndex >= v2Actions.size()) {
                v2Actions.add(delivery);
            }

            else {
                v2Actions.add(deliveryIndex, delivery);
            }
        }

        //after the task is successfully moved, we update the taskActionTimesMap.
        updateTimes(tempSolution, v1);
        updateTimes(tempSolution, v2);

        return tempSolution;
    }

    /**
     * updates the actionTimesMap for the new solution, this is because the
     * actionList of the vehicle v1 is changed and therefore the actionTimes
     * of all the tasks of the vehicle must be updated
     * @param tempSolution
     * @param v1
     */
    private void updateTimes(Solution tempSolution, Vehicle v1) {

        // we select the actionList of the vehicle v1
        List<Action> actions = tempSolution.getVehicleActionMap().get(v1);

        /*
         * we iterate on each action in the actionList and we update
         * the pickup time or the delivery time (depending on the type of the action)
         * of the task linked to the specific action
         */
        for (Action action : actions) {

            if (action.getActionType() == Action.ActionType.PICKUP) {
                tempSolution.getTaskActionTimesMap().get(action.getTask()).pickUpTime = actions.indexOf(action);
            }

            else {
                tempSolution.getTaskActionTimesMap().get(action.getTask()).deliveryTime = actions.indexOf(action);
            }
        }
    }


    /**
     * Creates a filtered list of valid solutions, using the isValid method of each solution
     * that checks the satisfaction of the constraints of the solution
     * @param neighbors
     */
    private ArrayList<Solution> filterOnConstraints(ArrayList<Solution> neighbors) {

        ArrayList<Solution> returnList = new ArrayList<>(neighbors);

        for (Solution neighbor : neighbors) {

            if (!neighbor.isValid()) {
                returnList.remove(neighbor);
            }
        }

        return returnList;
    }

    /**
     * First, it searches in the neighbor list the solution with the lower value of the objective function.
     * Then it will select with a probability p this new solution, otherwise it will select the old solution.
     * If the value of the objective function is the same as the objective function value of the previous solution
     * (previous iteration of the SLS) then a counter is incremented and, when this counter is equal to a selected
     * threshold, the method will choose a random Solution in the neighbor list. This last step is done
     * in order to not be stuck in a local minimum and try to find new solutions.
     */
    private Solution localChoice(List<Solution> neighbors, double probability, Solution oldSolution, int minimaThreshold) {

        Solution bestSolution = oldSolution;

        //we find the solution with the minimum cost
        for (Solution neighbor : neighbors) {
            if (neighbor.objectiveFunction() < bestSolution.objectiveFunction()) {
                bestSolution = neighbor;
            }
        }

        double rand = new Random().nextDouble();
        Solution solutionChosen;

        // we select the new solution with a certain probability
        if(rand < probability)
            solutionChosen = bestSolution;

        else
            solutionChosen = oldSolution;

        /*
          we check if the cost of the solution chosen is the same as the cost of
          the previous solution
         */
        if((int)solutionChosen.objectiveFunction() == (int)oldSolution.objectiveFunction()){
            equalCostCounter++;
        }
        else {
            equalCostCounter = 0;
        }

        /*
            if the counter is equal to a specific threshold we choose a random
            solution in the neighbor list. This is done in order to avoid local
            minimum.
         */
        if(equalCostCounter == minimaThreshold && !neighbors.isEmpty()){
            equalCostCounter = 0;
            int randIndex = new Random().nextInt(neighbors.size());
            solutionChosen = neighbors.get(randIndex);

        }

        return solutionChosen;
    }

    Solution localChoiceProbability(List<Solution> neighbors, double probability, Solution oldSolution){

        Solution bestSolution = oldSolution;

        //we find the solution with the minimum cost
        for (Solution neighbor : neighbors) {
            if (neighbor.objectiveFunction() < bestSolution.objectiveFunction()) {
                bestSolution = neighbor;
            }
        }

        double rand = new Random().nextDouble();

        // we select the new solution with a certain probability
        if(rand < probability)
            return bestSolution;

        else
            return oldSolution;

    }


    /**
     * We create a first naive valid solution
     * @return

    private Solution naiveSolution() {

    class LoadComparator implements Comparator<Vehicle> {

    @Override
    public int compare(Vehicle o1, Vehicle o2) {
    if(o1.capacity() > o2.capacity())
    return -1;

    if(o1.capacity() < o2.capacity())
    return 1;

    return 0;
    }
    }

    Solution solution = new Solution(tasksDomain, vehiclesDomain);

    for (Vehicle vehicle : vehiclesDomain) {

    solution.getVehicleTasksMap().put(vehicle, new ArrayList<>());
    solution.getVehicleActionMap().put(vehicle, new ArrayList<>());
    }

    ArrayList<Vehicle> vehicles = new ArrayList<>(vehiclesDomain);
    ArrayList<Task> tasksToAdd = new ArrayList<>(tasksDomain);
    Collections.sort(vehicles, new LoadComparator());

    // The vehicles are ordered following an descending order of load capacity.
    // We fill every vehicle in this order until the vehicle is full.
    for (Vehicle vehicle : vehicles) {

    if (tasksToAdd.isEmpty()) break;

    int load = 0;

    ArrayList<Task> vehicleTasks = new ArrayList<>();

    for (Task task : tasksDomain) {

    if(!tasksToAdd.contains(task))
    continue;

    if (task.weight <= vehicle.capacity() - load) {

    vehicleTasks.add(task);
    tasksToAdd.remove(task);

    // Updating the task->Vehicle map
    solution.getTaskVehicleMap().put(task, vehicle);

    load += task.weight;
    }
    else {
    break;
    }
    }

    // Updating the vehicle->List<Task> map
    solution.getVehicleTasksMap().put(vehicle, vehicleTasks);

    // We call this method that create all the actions of the vehicle and update their maps
    createActions(solution, vehicle, vehicleTasks);
    }

    return solution;

    }*/

}
