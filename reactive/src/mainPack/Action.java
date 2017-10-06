package mainPack;

import logist.topology.Topology.City;

/**
 * Created by lorenzotara on 05/10/17.
 */
public class Action {

    private City destination;

    public Action(City destination) {

        this.destination = destination;
    }


    public City getNextCity() {
        return destination;
    }


}
