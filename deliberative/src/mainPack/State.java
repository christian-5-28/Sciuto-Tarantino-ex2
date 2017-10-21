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
        if (availableTasks.size() == 0 && currentTasks.size() == 0) return false;

        if (action.getActionType() == Action.ActionType.PICKUP_AND_MOVE) {

            for (Task availableTask : availableTasks) {
                if (availableTask.pickupCity.equals(currentCity)
                        && availableCapacity >= availableTask.weight) {

                    return true;
                }
            }

            return false;
        }

        return true;
    }


}
