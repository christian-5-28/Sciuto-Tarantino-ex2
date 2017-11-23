package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

/**
 * Solution class: it model a solution for PDP as a COP (Constraint Optimization Problem).
 *
 * Variables:
 * 1) vehicleActions: Vehicle --> List of Actions (PICKUP or DELIVERY). It models the fact that a vehicle can carry multiple tasks in parallel
 * 2) TaskActionTimes: Task --> ActionTime: It models when a task is picked up and when it is delivered
 *
 * Domain:
 * 1) VehicleDomain: Vehicle set
 * 2) TaskDomain: Set of all possible tasks
 *
 * Constraints:
 * 1) loadConstraint: Vehicle --> boolean. (Details below)
 * 2) timeCostraint: Task --> boolean. (details below)
 * 3) allTasksDeliveredConstraint: verifies that all the possible tasks are picked up and delivered (details below)
 * 4) taskUnique: Task --> boolean. (details below)
 * 5) actionsAtDifferentTimesConstraint: returns true if every vehicle do all its actions in different times.
 *
 * Objective function: return the total cost of the solution (details below).
 *
 */

public class Solution {

    private Set<Task> tasksDomain;
    private List<Vehicle> vehiclesDomain;

    private Map<Task, ActionTimes> taskActionTimesMap;
    private Map<Vehicle, List<Action>> vehicleActionMap;



    public Solution(Set<Task> tasksDomain, List<Vehicle> vehiclesDomain) {

        this.tasksDomain = new HashSet<>();

        for (Task task : tasksDomain) {
            this.tasksDomain.add(task);
        }

        //this.tasksDomain = tasksDomain;
        this.vehiclesDomain = vehiclesDomain;

        taskActionTimesMap = new HashMap<>();
        vehicleActionMap = new HashMap<>();

        initializeMaps();
    }

    private void initializeMaps() {

        for (Vehicle vehicle : vehiclesDomain) {

            vehicleActionMap.put(vehicle, new ArrayList<>());
        }

    }

    /**
     * This constructor creates a new Solution object as a deep copy of the old solution
     * @param oldSolution
     */
    public Solution(Solution oldSolution) {

        this.vehicleActionMap = copyVehicleActions(oldSolution.vehicleActionMap);
        this.taskActionTimesMap = copyActionTimes(oldSolution.taskActionTimesMap);

        this.tasksDomain = oldSolution.tasksDomain;
        this.vehiclesDomain = oldSolution.vehiclesDomain;

    }

    /**
     * deep copy of actionTime value for each task
     * @param taskActionTimesMap
     * @return
     */
    private Map<Task, ActionTimes> copyActionTimes(Map<Task, ActionTimes> taskActionTimesMap) {

        HashMap<Task, ActionTimes> returnMap = new HashMap<>();

        for (Map.Entry<Task, ActionTimes> taskActionTimesEntry : taskActionTimesMap.entrySet()) {
            returnMap.put(taskActionTimesEntry.getKey(), new ActionTimes(taskActionTimesEntry.getValue()));
        }

        return returnMap;
    }

    /**
     * Deep Copy the Vehicle Actions map in the new Solution object
     * @param vehicleActionMap
     * @return
     */
    private Map<Vehicle, List<Action>> copyVehicleActions(Map<Vehicle, List<Action>> vehicleActionMap) {

        HashMap<Vehicle, List<Action>> returnMap = new HashMap<>();

        for (Map.Entry<Vehicle, List<Action>> vehicleListEntry : vehicleActionMap.entrySet()) {
            returnMap.put(vehicleListEntry.getKey(), new ArrayList<>(vehicleListEntry.getValue()));
        }

        return returnMap;
    }


    // CONSTRAINTS //

    /**
     * This constraint returns true iff one task is contained in
     * only one vehicle actionList and returns false otherwise
     */
    public boolean taskUnique(Task task) {

        boolean oneVehicle = false;
        // For every vehicle
        for (Vehicle vehicle : vehiclesDomain) {

            // We take all the actions
            for (Action action : vehicleActionMap.get(vehicle)) {

                // We check the task only one time (reason why there is actionType == PICKUP)
                if (action.getActionType() == Action.ActionType.PICKUP && action.getTask().equals(task)) {
                    // If the task was never found before, it means that one vehicle has it
                    if (!oneVehicle) oneVehicle = true;
                    // Else, the task has already been found and it has been found again
                    else return false;
                }
            }
        }

        return oneVehicle;
    }


    /**
     * This function returns true if every vehicle do all its actions in different times
     * @return
     */
    public boolean actionsAtDifferentTimesConstraint() {

        boolean returnValue = true;

        // For every vehicle we get all the actions
        for (Vehicle vehicle : vehiclesDomain) {
            List<Action> actionList = vehicleActionMap.get(vehicle);

            ArrayList<Integer> actionTimesList = new ArrayList<>();
            for (Action action : actionList) {

                // For every task in the actionList of the vehicle we take the actionTimes - pickUp and delivery
                // and we add it to the list of actionTimesList of the vehicle
                if (action.getActionType() == Action.ActionType.PICKUP) {
                    Task task = action.getTask();

                    ActionTimes actionTimes = taskActionTimesMap.get(task);
                    actionTimesList.add(actionTimes.pickUpTime);
                    actionTimesList.add(actionTimes.deliveryTime);
                }
            }

            Set<Integer> actionTimesSet = new HashSet<>(actionTimesList);

            // If the set created by the actionTimesList has the same size of the list
            // it means that no value was repeated
            returnValue = returnValue && actionTimesSet.size() == actionTimesList.size();
        }

        return returnValue;
    }


    /**
     * constraint on the fact that, in every step of our vehicle,
     * the total weight of the carried tasks must be lower than the
     * capacity of the vehicle.
     * @param vehicle
     * @return
     */
    public boolean loadConstraint(Vehicle vehicle){

        List<Action> actionsList = vehicleActionMap.get(vehicle);

        int currentLoad = 0;

        /*
        for each action step, we verify that the current load is
        lower than the vehicle capacity, then we update the current load
         */
        for (Action action : actionsList) {

            if(currentLoad > vehicle.capacity())
                return false;

            switch (action.getActionType()){
                case PICKUP:
                    currentLoad += action.getTask().weight;
                    break;
                case DELIVERY:
                    currentLoad -= action.getTask().weight;
            }
            
        }

        return true;
    }

    /**
     * this constraint checks if the time for the pickUp of a task
     * is strictly lower than the delivery time of that task
     * @param task
     * @return
     */
    public boolean timeConstraint(Task task){

        ActionTimes actionTimes = taskActionTimesMap.get(task);

        if(actionTimes == null){
            int i = 0;
        }


        return actionTimes.pickUpTime < actionTimes.deliveryTime;

    }

    /**
     * this constraint checks if the the number of tasks picked up and delivered is equal
     * to all the existing tasks
     * @return
     */
    public boolean allTasksDeliveredConstraint() {

       for (Task task : taskActionTimesMap.keySet()) {
            if(!tasksDomain.contains(task)){
                return false;
            }
        }
        return true;

    }


    /**
     * returns true if every constraint is respected
     * @return
     */
    public boolean isValid() {

        boolean returnValue = true;

        for (Task task : tasksDomain) {
            returnValue = returnValue && timeConstraint(task);
            returnValue = returnValue && taskUnique(task);
        }

        for (Vehicle vehicle : vehiclesDomain) {
            returnValue = returnValue && loadConstraint(vehicle);
        }
        returnValue = returnValue && allTasksDeliveredConstraint();

        return returnValue && allTasksDeliveredConstraint() && actionsAtDifferentTimesConstraint();
    }

    /**
     * computes the total cost for this Solution, that is the
     * sum of all the vehicle costs.
     * @return
     */
    public double objectiveFunction(){

        double totalCost = 0.;

        for (Vehicle vehicle : vehiclesDomain) {
            totalCost += vehicleCost(vehicle);
        }

        return totalCost;
    }

    /**
     * computes the vehicle cost simulating is total path considering its actionsList.
     * It assumes that the solution is valid
     */
    private double vehicleCost(Vehicle vehicle) {

        List<Action> vehicleActions = vehicleActionMap.get(vehicle);

        if(vehicleActions != null && !vehicleActions.isEmpty() ){

            // TODO: vehicle.getCurrentCity() = null
            // cost initialized with the cost to go to the first pickup city from homeCity of the vehicle
            double cost = vehicle.getCurrentCity().distanceTo(vehicleActions.get(0).getTask().pickupCity) * vehicle.costPerKm();

            for (int i = 0; i < vehicleActions.size() - 1; i++) {

                Action currentAction = vehicleActions.get(i);
                Action nextAction = vehicleActions.get(i + 1);
                Topology.City currentCity;
                Topology.City nextCity;


                if(currentAction.getActionType() == Action.ActionType.PICKUP) {
                    currentCity = currentAction.getTask().pickupCity;
                }
                else {
                    currentCity = currentAction.getTask().deliveryCity;
                }

                if(nextAction.getActionType() == Action.ActionType.PICKUP) {
                    nextCity = nextAction.getTask().pickupCity;
                }
                else {
                    nextCity = nextAction.getTask().deliveryCity;
                }

                cost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();
            }

            return cost;
        }

        return 0.;
    }

   public List<Vehicle> getVehiclesDomain() {
        return vehiclesDomain;
    }


    public Map<Task, ActionTimes> getTaskActionTimesMap() {
        return taskActionTimesMap;
    }

    public Map<Vehicle, List<Action>> getVehicleActionMap() {
        return vehicleActionMap;
    }

    public Set<Task> getTaskDomain() {
        return tasksDomain;
    }

    /**
     * returns the vehicle that has to pickup and deliver the
     * specific task.
     */
    public Vehicle getVehicle(Task task){
        int taskIndex = taskActionTimesMap.get(task).pickUpTime;

        for (Map.Entry<Vehicle, List<Action>> vehicleListEntry : vehicleActionMap.entrySet()) {
            Vehicle vehicle = vehicleListEntry.getKey();
            List<Action> actionList = vehicleListEntry.getValue();

            if(taskIndex < actionList.size() && task.id == actionList.get(taskIndex).getTask().id){
                return vehicle;
            }
        }

        return null;
    }
}
