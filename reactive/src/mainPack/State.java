package mainPack;

import logist.topology.Topology.City;

/**
 * Created by lorenzotara on 05/10/17.
 */
public class State {

    private City startingCity;
    private City destination;

    private boolean taskPresent;



    public State(City startingCity, City destination, boolean taskPresent) {
        this.startingCity = startingCity;
        this.destination = destination;
        this.taskPresent = taskPresent;
    }



    public City getStartingCity() {
        return startingCity;
    }

    public void setStartingCity(City startingCity) {
        this.startingCity = startingCity;
    }

    public City getDestination() {
        return destination;
    }

    public void setDestination(City destination) {
        this.destination = destination;
    }

    public boolean isTaskPresent() {
        return taskPresent;
    }

    public void setTaskPresent(boolean taskPresent) {
        this.taskPresent = taskPresent;
    }
}
