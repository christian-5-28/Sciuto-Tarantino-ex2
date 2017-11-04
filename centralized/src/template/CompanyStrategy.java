package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

/**
 * Created by lorenzotara on 03/11/17.
 */
public class CompanyStrategy {

    private TaskSet tasksDomain;
    private List<Vehicle> vehiclesDomain;

    public CompanyStrategy(TaskSet tasksDomain, List<Vehicle> vehiclesDomain) {
        this.tasksDomain = tasksDomain;
        this.vehiclesDomain = vehiclesDomain;
    }

    public Solution SLS(int maxIter, double probability) {

        Solution solution = initialSolution();
        //Solution solution = naiveSolution();

        for (int i = 0; i < maxIter; i++) {

            System.out.println("iteration: " + i);
            Solution oldSolution = new Solution(solution);
            List<Solution> neighbors = chooseNeighbors(oldSolution);
            solution = localChoice(neighbors, probability, oldSolution);
            System.out.println("solution cost: " + solution.objectiveFunction());

        }

        return solution;

    }

    private Solution localChoice(List<Solution> neighbors, double probability, Solution oldSolution) {

        Solution bestSolution = oldSolution;

        for (Solution neighbor : neighbors) {
            if (neighbor.objectiveFunction() < bestSolution.objectiveFunction()) {
                bestSolution = neighbor;
            }
        }

        double rand = new Random().nextDouble();

        if(rand <= probability)
            return bestSolution;

        if (rand > probability && rand <= 2*probability)
            return oldSolution;

        int randIndex = new Random().nextInt(neighbors.size());

        return neighbors.get(randIndex);

    }

    private Solution initialSolution(){

        Solution solution = new Solution(tasksDomain, vehiclesDomain);

        for (Vehicle vehicle : vehiclesDomain) {

            solution.getVehicleTasksMap().put(vehicle, new ArrayList<>());
            solution.getVehicleActionMap().put(vehicle, new ArrayList<>());
        }


        ArrayList<Vehicle> vehicles = new ArrayList<>(vehiclesDomain);
        ArrayList<Task> tasksToAdd = new ArrayList<>(tasksDomain);

        int numberOfvehicles = vehicles.size();

        int vehicleIndex = 0;

        for (Vehicle vehicle : vehicles) {

            for (Task task : tasksDomain) {
                if(task.pickupCity.equals(vehicle.homeCity()) && task.weight <= vehicle.capacity() - vehicle.getCurrentTasks().weightSum()){

                    solution.getVehicleTasksMap().get(vehicle).add(task);
                    tasksToAdd.remove(task);
                }
            }

        }
        //TODO: while non vuota
        for (Task task : tasksToAdd) {

            Vehicle vehicle = vehicles.get(vehicleIndex);
            vehicleIndex = (vehicleIndex + 1 ) % numberOfvehicles;
            if(task.weight <= vehicle.capacity() - vehicle.getCurrentTasks().weightSum()){
                solution.getVehicleTasksMap().get(vehicle).add(task);
            }
        }

        for (Vehicle vehicle : vehicles) {

            List<Task> vehicleTasks = solution.getVehicleTasksMap().get(vehicle);

            createActions(solution, vehicle, vehicleTasks);

        }

        return solution;

    }

    /**
     * We create a first naive valid solution
     * @return
     */
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

    }

    /**
     * For every task of the vehicle, we create two actions (pickUp and delivery) and we put them in order
     * in the actionList that is the value of the map that has as key the vehicle.
     * Then we update for each task the times of pickUp of delivery.
     * @param solution
     * @param vehicle
     * @param vehicleTasks
     */
    private void createActions(Solution solution, Vehicle vehicle, List<Task> vehicleTasks) {

        ArrayList<Action> actions = new ArrayList<>();

        ArrayList<Task> tasksToAdd = new ArrayList<>(vehicleTasks);
        Topology.City currentCity = vehicle.getCurrentCity();

        ArrayList<Task> tasksToDeliver = new ArrayList<>();

        for (Task task : tasksToAdd) {
            if (task.pickupCity.equals(currentCity)) {
                Action pickUp = new Action(Action.ActionType.PICKUP, task);
                actions.add(pickUp);
                tasksToDeliver.add(task);
            }
        }

        for (Task task : tasksToDeliver) {

            Action delivery = new Action(Action.ActionType.DELIVERY, task);
            actions.add(delivery);
            tasksToAdd.remove(task);

            int indexOfPickup = tasksToDeliver.indexOf(task);

            solution.getTaskActionTimesMap().put(task, new ActionTimes(indexOfPickup, indexOfPickup + tasksToDeliver.size()));
        }

        for (Task task: tasksToAdd) {

            Action pickUp = new Action(Action.ActionType.PICKUP, task);
            Action delivery = new Action(Action.ActionType.DELIVERY, task);
            actions.add(pickUp);
            actions.add(delivery);

            solution.getTaskActionTimesMap().put(task, new ActionTimes(actions.indexOf(pickUp), actions.indexOf(delivery)));

        }

        solution.getVehicleActionMap().put(vehicle, actions);
    }

    public List<Solution> chooseNeighbors(Solution oldSolution) {

        ArrayList<Solution> neighbors = new ArrayList<>();

        // Getting random vehicle from the vehicle domain
        List<Vehicle> vehiclesDomain = oldSolution.getVehiclesDomain();

        List<Vehicle> vehiclesWithTask = new ArrayList<>();
        for (Vehicle vehicle : vehiclesDomain) {
            if(!oldSolution.getVehicleTasksMap().get(vehicle).isEmpty()){
                vehiclesWithTask.add(vehicle);
            }
        }

        int randVehicleIndex = new Random().nextInt(vehiclesWithTask.size());
        Vehicle randVehicle = vehiclesWithTask.get(randVehicleIndex);

        // Getting random task from the task domain of the vehicle
        List<Task> vehicleTasks = oldSolution.getVehicleTasksMap().get(randVehicle);
        int randTaskIndex = new Random().nextInt(vehicleTasks.size());
        Task taskToSwitch = oldSolution.getVehicleTasksMap().get(randVehicle).get(randTaskIndex);

        // For every vehicle in the domain, if the vehicle is != randVehicle, then
        // we switch the random task between the two vehicles and we create
        // a new candidate solution

        System.out.println("starting switch task");
        for (Vehicle vehicle : oldSolution.getVehiclesDomain()) {

            // TODO: vedere se aggiungere come condizione dell'if il controllo della free capacity del vehicle
            // TODO: a cui viene aggiunta la task rispetto alla task che gli stiamo dando
            if(vehicle.id() != randVehicle.id()){
                neighbors.add(changingVehicle(oldSolution, randVehicle, vehicle, taskToSwitch));
            }
        }

        // If the current vehicle has more than one task, we compute all the possible
        // solutions considering all the possible permutations of the actions.
        // Then we add them to the neighbors solution List

        System.out.println("starting permutation");
        /*if (oldSolution.getVehicleTasksMap().get(randVehicle).size() >= 2) {
            neighbors.addAll(actionPermutation(oldSolution, randVehicle));
        }*/

        List<Action> vehicleActionsList = oldSolution.getVehicleActionMap().get(randVehicle);
        int vehicleActionsListSize = vehicleActionsList.size();
        if(vehicleActionsListSize > 2){
            for(int firstIndex = 0; firstIndex < vehicleActionsListSize - 1; firstIndex++){
                for(int secondIndex = firstIndex + 1; secondIndex < vehicleActionsListSize; secondIndex++){
                    neighbors.add(swapActions(oldSolution, randVehicle, vehicleActionsList, firstIndex, secondIndex));
                }
            }
        }

        neighbors = filterOnConstraints(neighbors);

        return neighbors;

    }

    private Solution swapActions(Solution oldSolution, Vehicle vehicle, List<Action> vehicleActionsList, int firstIndex, int secondIndex) {

        Solution newSolution = new Solution(oldSolution);

        Action firstAction = vehicleActionsList.get(firstIndex);
        Action secondAction = vehicleActionsList.get(secondIndex);

        newSolution.getVehicleActionMap().get(vehicle).set(firstIndex, secondAction);
        newSolution.getVehicleActionMap().get(vehicle).set(secondIndex, firstAction);

        updateTimes(newSolution, vehicle);

        return newSolution;

    }


    public Solution changingVehicle(Solution oldSolution, Vehicle v1, Vehicle v2, Task taskToSwitch) {

        Solution tempSolution = new Solution(oldSolution);

        // Changing the value of the map task -> vehicle from t->v1 to t->v2
        tempSolution.getTaskVehicleMap().put(taskToSwitch, v2);

        // Adding t1 to the list of tasks of v2
        // In vehicle tasksMap it's mandatory to have every vehicle as key
        // Removing t1 from the list of tasks of v1
        tempSolution.getVehicleTasksMap().get(v2).add(taskToSwitch);
        tempSolution.getVehicleTasksMap().get(v1).remove(taskToSwitch);

        // We get the actionTimes (indexes of pickup and delivery of the task in the vehicle action list)
        ActionTimes actionTimes = tempSolution.getTaskActionTimesMap().get(taskToSwitch);

        // We remove from the vehicle (v1) the actions that it is not going to do anymore
        Action pickUp = tempSolution.getVehicleActionMap().get(v1).get(actionTimes.pickUpTime);
        Action delivery = tempSolution.getVehicleActionMap().get(v1).get(actionTimes.deliveryTime);
        tempSolution.getVehicleActionMap().get(v1).remove(actionTimes.pickUpTime);
        tempSolution.getVehicleActionMap().get(v1).remove(actionTimes.deliveryTime - 1);

        // We add those two actions to the vehicle v2
        //tempSolution.getVehicleActionMap().get(v2).add(pickUp);
        //tempSolution.getVehicleActionMap().get(v2).add(delivery);

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
     * Taking the current actions of randVehicle, we compose all the permutations of the actions
     * and we return all of them in a List of List of solutions composed by every permutation
     * @param oldSolution
     * @param randVehicle
     * @return
     */
    private ArrayList<Solution> actionPermutation(Solution oldSolution, Vehicle randVehicle) {

        List<Action> actions = oldSolution.getVehicleActionMap().get(randVehicle);

        // This will be the wrapper of all the permutations
        ArrayList<List<Action>> permutations = new ArrayList<>();

        permuteHelper(actions, 0, permutations);

        // This will be the wrapper of all the solutions
        ArrayList<Solution> solutions = new ArrayList<>();

        // Here we create one solution for each permutation
        for (List<Action> permutation : permutations) {

            Solution newSolution = new Solution(oldSolution);
            newSolution.getVehicleActionMap().put(randVehicle, permutation);
            updateTimes(newSolution, randVehicle);

            solutions.add(newSolution);
        }

        return solutions;

    }

    /**
     * Recursive function that compute all the permutations for a list of actions
     * @param actionList
     * @param index
     * @param blankList
     */
    private static void permuteHelper(List<Action> actionList, int index, ArrayList<List<Action>> blankList){

        //If we are at the last element - nothing left to permute
        if (index >= actionList.size() - 1) {

            blankList.add(new ArrayList<>(actionList));
            return;
        }

        //For each index in the sub array arr[index...end]
        for (int i = index; i < actionList.size(); i++) {

            //Swap the elements at indices index and i
            Action temp = actionList.get(index);
            actionList.set(index, actionList.get(i));
            actionList.set(i, temp);

            //Recurse on the sub array arr[index+1...end]
            permuteHelper(actionList, index+1, blankList);

            //Swap the elements back
            temp = actionList.get(index);
            actionList.set(index, actionList.get(i));
            actionList.set(i, temp);
        }
    }

    /**
     * We create a filtered list of valid solutions
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

}
