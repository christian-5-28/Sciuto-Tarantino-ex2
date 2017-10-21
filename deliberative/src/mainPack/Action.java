package mainPack;

import logist.topology.Topology.City;

/**
 * Created by lorenzotara on 20/10/17.
 */
public class Action {

    enum ActionType {
        PICKUP_AND_MOVE,
        MOVE
    }

    private City destination;
    private ActionType actionType;

    public Action(City destination, ActionType actionType) {
        this.destination = destination;
        this.actionType = actionType;
    }

    public City getDestination() {
        return destination;
    }

    public ActionType getActionType() {
        return actionType;
    }
}
