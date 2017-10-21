package mainPack;

import logist.topology.Topology.City;

public class Action {

    private City destination;

    public Action(City destination) {

        this.destination = destination;
    }


    public City getNextCity() {
        return destination;
    }


}
