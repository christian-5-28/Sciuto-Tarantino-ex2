package mainPack;

import logist.task.Task;
import logist.topology.Topology.City;

import java.util.List;

/**
 * Created by lorenzotara on 20/10/17.
 */
public class State {

    private City currentCity;
    private List<Task> availableTasks;
    private List<Task> currentTasks;
    private int availableCapacity;

    public State(City currentCity, List<Task> availableTasks, List<Task> currentTasks, int availableCapacity) {
        this.currentCity = currentCity;
        this.availableTasks = availableTasks;
        this.currentTasks = currentTasks;
        this.availableCapacity = availableCapacity;
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

    public boolean isActionPossible(Action action) {

        if (action.getDestination().equals(currentCity)) return false;

        // Goal state
        if (availableTasks.isEmpty() && currentTasks.isEmpty()) return false;

        int currentCapacity = availableCapacity;

        // Adding more free weight if the agent is delivering a task
        for (Task task : currentTasks) {
            if (task.deliveryCity.equals(action.getDestination())) {
                currentCapacity += task.weight;
            }
        }

        if (action.isPickup()) {

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


}
