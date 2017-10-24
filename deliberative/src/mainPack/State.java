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
    private List<Task> carriedTasks;
    //private List<Action> actionsAlreadyExecuted;

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
        return getCarriedTasks() != null ? getCarriedTasks().equals(state.getCarriedTasks()) : state.getCarriedTasks() == null;
    }

    @Override
    public int hashCode() {
        int result = getAvailableCapacity();
        result = 31 * result + (getCurrentCity() != null ? getCurrentCity().hashCode() : 0);
        result = 31 * result + (getAvailableTasks() != null ? getAvailableTasks().hashCode() : 0);
        result = 31 * result + (getCarriedTasks() != null ? getCarriedTasks().hashCode() : 0);
        return result;
    }


    /*public State(City currentCity, List<Task> availableTasks, List<Task> carriedTasks,
               List<Action> actionsAlreadyExecuted, int availableCapacity, double distanceCost) {
        this.currentCity = currentCity;
        this.availableTasks = availableTasks;
        this.carriedTasks = carriedTasks;
        //this.actionsAlreadyExecuted = actionsAlreadyExecuted;
        this.availableCapacity = availableCapacity;
        this.distanceCost = distanceCost;
    }*/

    public State(City currentCity, List<Task> availableTasks, List<Task> carriedTasks,
                  int availableCapacity, double distanceCost) {
        this.currentCity = currentCity;
        this.availableTasks = availableTasks;
        this.carriedTasks = carriedTasks;
        this.availableCapacity = availableCapacity;
        this.distanceCost = distanceCost;
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public List<Task> getAvailableTasks() {
        return availableTasks;
    }

    public List<Task> getCarriedTasks() {
        return carriedTasks;
    }

    public int getAvailableCapacity() {
        return availableCapacity;
    }



    /*public boolean isActionPossible(DeliberativeAction deliberativeAction) {

        if (deliberativeAction.getDestination().equals(currentCity)) return false;

        // Goal state
        if (availableTasks.isEmpty() && carriedTasks.isEmpty()) return false;

        int currentCapacity = availableCapacity;

        if (deliberativeAction.isPickup()) {

            for (Task availableTask : availableTasks) {
                if (availableTask.pickupCity.equals(currentCity)
                        && currentCapacity >= availableTask.weight) {

                    return true;
                }
            }

        /* Adding more free weight if the agent is delivering a task
        for (Task task : carriedTasks) {
            if (task.deliveryCity.equals(deliberativeAction.getDestination())) {
                currentCapacity += task.weight;
            }
        }

            return false;
        }

        return true;
    }*/


    /*public List<Action> getActionsAlreadyExecuted() {
        return actionsAlreadyExecuted;
    }*/

    public double getDistanceCost() {
        return distanceCost;
    }
}
