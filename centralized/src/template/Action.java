package template;

import logist.task.Task;

/**
 Action class: it models the vehicle action.
 1) Two different action type: PICKUP and DELIVERY
 2) the specific task taken by the vehicle
 */
public class Action {

    enum ActionType {
        PICKUP,
        DELIVERY
    }
    ActionType actionType;
    Task task;

    public Action(ActionType actionType, Task task) {
        this.actionType = actionType;
        this.task = task;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public Task getTask() {
        return task;
    }
}
