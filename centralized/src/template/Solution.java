package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by lorenzotara on 03/11/17.
 */
public class Solution {

    private TaskSet tasksDomain;
    private Set<Vehicle> vehiclesDomain;

     class ActionTimes {

        public int pickUpTime;
        public int deliveryTime;
    }

     private Map<Vehicle, TaskSet> vehicleTasksMap;
     private Map<Task, ActionTimes> taskActionTimesMap;
     private Map<Vehicle, List<Action>> vehicleActionMap;
     private Map<Task, Vehicle> taskVehicleMap;

    public Solution(TaskSet tasksDomain, Set<Vehicle> vehiclesDomain) {

        this.tasksDomain = tasksDomain;
        this.vehiclesDomain = vehiclesDomain;

        vehicleTasksMap = new HashMap<>();
        taskActionTimesMap = new HashMap<>();
        vehicleActionMap = new HashMap<>();
        taskVehicleMap = new HashMap<>();
    }

    // COSTRAINT
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

        return actionTimes.pickUpTime < actionTimes.deliveryTime;

    }

    /**
     * this constraint checks if the the number of tasks picked up and delivered is equal
     * to all the existing tasks
     * @return
     */
    public boolean allTasksDeliveredConstraint() {

        return tasksDomain == taskActionTimesMap.keySet();

    }


    /**
     * checks if a task is linked with a vehicle, then
     * the task is inside the taskSet linked to that vehicle
     * @param task
     * @return
     */
    /*public boolean constraint3(Task task){

        Vehicle vehicle = vehicleMap.get(task);

        return vehicleTasksMap.get(vehicle).contains(task);

    }*/

    /**
     * alternative version
     */
    /*public boolean constraint3Alternative(Vehicle vehicle){

        for (Task task : vehicleTasksMap.get(vehicle)) {

            if(vehicleMap.get(task).id() != vehicle.id())
                return false;

        }

        return true;
    }*/

    //TODO: objective function
    public double objectiveFunction(){

        double totalCost = 0.;

        for (Vehicle vehicle : vehiclesDomain) {
            totalCost += vehicleCost(vehicle);
        }

        return totalCost;
    }

    private double vehicleCost(Vehicle vehicle) {

        List<Action> vehicleActions = vehicleActionMap.get(vehicle);

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
}
