package mainPack;

import logist.plan.Action;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.Comparator;
import java.util.List;

/**
 * Created by lorenzotara on 20/10/17.
 */
public class State {

    private int availableCapacity;
    private double distanceCost;
    private double totalCost;

    private City currentCity;
    private List<Task> availableTasks;
    private List<Task> currentTasks;
    private double heuristic;

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        if (getAvailableCapacity() != state.getAvailableCapacity()) return false;
        if (getCurrentCity() != null ? !getCurrentCity().equals(state.getCurrentCity()) : state.getCurrentCity() != null)
            return false;
        if (getAvailableTasks() != null ? !getAvailableTasks().equals(state.getAvailableTasks()) : state.getAvailableTasks() != null)
            return false;
        return getCurrentTasks() != null ? getCurrentTasks().equals(state.getCurrentTasks()) : state.getCurrentTasks() == null;
    }

    @Override
    public int hashCode() {
        int result = getAvailableCapacity();
        result = 31 * result + (getCurrentCity() != null ? getCurrentCity().hashCode() : 0);
        result = 31 * result + (getAvailableTasks() != null ? getAvailableTasks().hashCode() : 0);
        result = 31 * result + (getCurrentTasks() != null ? getCurrentTasks().hashCode() : 0);
        return result;
    }

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



    /*public boolean isActionPossible(DeliberativeAction deliberativeAction) {

        if (deliberativeAction.getDestination().equals(currentCity)) return false;

        // Goal state
        if (availableTasks.isEmpty() && currentTasks.isEmpty()) return false;

        int currentCapacity = availableCapacity;

        if (deliberativeAction.isPickup()) {

            for (Task availableTask : availableTasks) {
                if (availableTask.pickupCity.equals(currentCity)
                        && currentCapacity >= availableTask.weight) {

                    return true;
                }
            }

        /* Adding more free weight if the agent is delivering a task
        for (Task task : currentTasks) {
            if (task.deliveryCity.equals(deliberativeAction.getDestination())) {
                currentCapacity += task.weight;
            }
        }

            return false;
        }

        return true;
    }*/


    public static Comparator<State> compareByTotalCost = new Comparator<State>() {
        @Override
        public int compare(State s1, State s2) {
            return s1.compareTo(s2);
        }
    };

    public int compareTo(State state) {
        // descending
        return (int) (this.totalCost - state.totalCost);

    }


    public List<Action> getActionsAlreadyExecuted() {
        return actionsAlreadyExecuted;
    }

    public double getDistanceCost() {
        return distanceCost;
    }

    public void updateTotalCost(double heuristic) {
        this.heuristic = heuristic;
        this.totalCost = heuristic + distanceCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getHeuristic() {
        return heuristic;
    }
}
