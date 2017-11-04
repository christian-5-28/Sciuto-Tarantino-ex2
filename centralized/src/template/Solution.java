package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

/**
 * Created by lorenzotara on 03/11/17.
 */
public class Solution {

    private static TaskSet tasksDomain;
    private static List<Vehicle> vehiclesDomain;


     private Map<Vehicle, List<Task>> vehicleTasksMap;
     private Map<Task, ActionTimes> taskActionTimesMap;
     private Map<Vehicle, List<Action>> vehicleActionMap;
     private Map<Task, Vehicle> taskVehicleMap;

    /**
     * This constructor creates a new Solution object as a deep copy of the old solution
     * @param oldSolution
     */
    public Solution(Solution oldSolution) {

        this.vehicleTasksMap = copyVehicleTasks(oldSolution.vehicleTasksMap);
        this.vehicleActionMap = copyVehicleActions(oldSolution.vehicleActionMap);
        this.taskActionTimesMap = copyActionTimes(oldSolution.taskActionTimesMap);
        this.taskVehicleMap = new HashMap<>(oldSolution.getTaskVehicleMap());


    }

    private Map<Task, ActionTimes> copyActionTimes(Map<Task, ActionTimes> taskActionTimesMap) {

        HashMap<Task, ActionTimes> returnMap = new HashMap<>();

        for (Map.Entry<Task, ActionTimes> taskActionTimesEntry : taskActionTimesMap.entrySet()) {
            returnMap.put(taskActionTimesEntry.getKey(), new ActionTimes(taskActionTimesEntry.getValue()));
        }

        return returnMap;
    }

    /**
     * Copying the Vehicle Actions map
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


    /**
     * Copying the Vehicle Tasks map
     * @param vehicleTasksMap
     * @return
     */
    private Map<Vehicle, List<Task>> copyVehicleTasks(Map<Vehicle, List<Task>> vehicleTasksMap) {

        HashMap<Vehicle, List<Task>> returnMap = new HashMap<>();

        for (Map.Entry<Vehicle, List<Task>> vehicleListEntry : vehicleTasksMap.entrySet()) {
            returnMap.put(vehicleListEntry.getKey(), new ArrayList<>(vehicleListEntry.getValue()));
        }

        return returnMap;
    }

    public Solution(TaskSet tasksDomain, List<Vehicle> vehiclesDomain) {

        Solution.tasksDomain = tasksDomain;
        Solution.vehiclesDomain = vehiclesDomain;

        vehicleTasksMap = new HashMap<>();
        taskActionTimesMap = new HashMap<>();
        vehicleActionMap = new HashMap<>();
        taskVehicleMap = new HashMap<>();
    }

    // CONSTRAINT
    /**
     * constraint on the fact that each task of the vehicle has a weight lower
     * then the capacity of the vehicle and also that the sum of the wieghts
     * is lower than the capacity
     * @param vehicle
     * @return
     */
    public boolean loadConstraint(Vehicle vehicle){

        int totalWeight = 0;
        for (Task task : vehicleTasksMap.get(vehicle)) {

            totalWeight += task.weight;
        }

        return totalWeight < vehicle.capacity();
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

        //TODO: controllare che esista il tempo di pick up e delivery

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
        }

        for (Vehicle vehicle : vehiclesDomain) {
            returnValue = returnValue && loadConstraint(vehicle);
        }
        returnValue = returnValue && allTasksDeliveredConstraint();

        return returnValue && allTasksDeliveredConstraint();
    }


    public double objectiveFunction(){

        double totalCost = 0.;

        for (Vehicle vehicle : vehiclesDomain) {
            totalCost += vehicleCost(vehicle);
        }

        return totalCost;
    }

    private double vehicleCost(Vehicle vehicle) {

        List<Action> vehicleActions = vehicleActionMap.get(vehicle);

        if(!vehicleActions.isEmpty()){

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

    public TaskSet getTasksDomain() {
        return tasksDomain;
    }

    public List<Vehicle> getVehiclesDomain() {
        return vehiclesDomain;
    }

    public Map<Vehicle, List<Task>> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public Map<Task, ActionTimes> getTaskActionTimesMap() {
        return taskActionTimesMap;
    }

    public Map<Vehicle, List<Action>> getVehicleActionMap() {
        return vehicleActionMap;
    }

    public Map<Task, Vehicle> getTaskVehicleMap() {
        return taskVehicleMap;
    }
}
