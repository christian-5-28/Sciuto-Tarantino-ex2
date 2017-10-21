package mainPack;

import logist.topology.Topology.City;

/**
 * Created by lorenzotara on 20/10/17.
 */
public class Action {

    /*enum ActionType {
        PICKUP_AND_MOVE,
        MOVE
    }*/

    private City destination;
    private boolean pickup;

    public Action(City destination, boolean pickup) {
        this.destination = destination;
        this.pickup = pickup;
    }

    public City getDestination() {
        return destination;
    }

    public boolean isPickup() {
        return pickup;
    }
}
