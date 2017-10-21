package mainPack;

import logist.plan.Action;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.List;

/**
 * Created by lorenzotara on 20/10/17.
 */
public class State {

    private int availableCapacity;
    private double distanceCost;

    private City currentCity;
    private List<Task> availableTasks;
    private List<Task> currentTasks;
    private List<Action> actionsAlreadyExecuted;


    public State(City currentCity, List<Task> availableTasks, List<Task> currentTasks,
                 List<Action> actionsAlreadyExecuted, int availableCapacity, double distanceCost) {
        this.currentCity = currentCity;
        this.availableTasks = availableTasks;
        this.currentTasks = currentTasks;
        this.actionsAlreadyExecuted = actionsAlreadyExecuted;
        this.availableCapacity = availableCapacity;
        this.distanceCost = distanceCost;
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public List<Task> getAvailableTasks() {
        return availableTasks;
    }

    public List<Task> getCurrentTasks() {
        return currentTasks;
    }

    public int getAvailableCapacity() {
        return availableCapacity;
    }

    public boolean isActionPossible(DeliberativeAction deliberativeAction) {

        if (deliberativeAction.getDestination().equals(currentCity)) return false;

        // Goal state
        if (availableTasks.isEmpty() && currentTasks.isEmpty()) return false;

        int currentCapacity = availableCapacity;

        // Adding more free weight if the agent is delivering a task
        for (Task task : currentTasks) {
            if (task.deliveryCity.equals(deliberativeAction.getDestination())) {
                currentCapacity += task.weight;
            }
        }

        if (deliberativeAction.isPickup()) {

            for (Task availableTask : availableTasks) {
                if (availableTask.pickupCity.equals(currentCity)
                        && currentCapacity >= availableTask.weight) {

                    return true;
                }
            }

            return false;
        }

        return true;
    }


    public List<Action> getActionsAlreadyExecuted() {
        return actionsAlreadyExecuted;
    }

    public double getDistanceCost() {
        return distanceCost;
    }
}
