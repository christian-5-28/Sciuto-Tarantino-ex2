package template;

import logist.task.Task;

/**
 * Created by lorenzotara on 03/11/17.
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
