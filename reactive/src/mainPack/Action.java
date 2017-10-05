package mainPack;

import logist.topology.Topology.City;

/**
 * Created by lorenzotara on 05/10/17.
 */
public class Action {

    enum ActionType {

        PICKUP,
        MOVE
    }

    private ActionType actionType;
    private City destination;

    public Action(ActionType actionType, City destination) {
        this.actionType = actionType;
        this.destination = destination;
    }

    //TODO: executeAction()

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public City getDestination() {
        return destination;
    }

    public void setDestination(City destination) {
        this.destination = destination;
    }
}
