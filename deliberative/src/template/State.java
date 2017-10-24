package template;

import logist.task.Task;
import logist.topology.Topology.City;

import java.util.List;

public class State {

    private int availableCapacity;

    private City currentCity;
    private List<Task> availableTasks;
    private List<Task> carriedTasks;


    public State(City currentCity, List<Task> availableTasks, List<Task> carriedTasks,
                 int availableCapacity) {
        this.currentCity = currentCity;
        this.availableTasks = availableTasks;
        this.carriedTasks = carriedTasks;
        this.availableCapacity = availableCapacity;
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

    /*public double getDistanceCost() {
        return distanceCost;
    }
*/
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

}
