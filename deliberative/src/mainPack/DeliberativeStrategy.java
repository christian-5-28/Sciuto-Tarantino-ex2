package mainPack;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.*;

/**
 * Created by lorenzotara on 20/10/17.
 */
public class DeliberativeStrategy {

    private Node root;
    private List<Action> actionList;


    public Plan astar(Vehicle vehicle, TaskSet tasks) {
        createTree(vehicle, tasks);
        return null;
    }

    public Plan bfs(Vehicle vehicle, TaskSet tasks) {
        createTree(vehicle, tasks);
        return null;
    }

    public void createTree(Vehicle vehicle, TaskSet tasks) {

        City currentCity = vehicle.getCurrentCity();
        List<Task> availableTasks = new ArrayList<>(tasks);
        List<Task> currentTasks = new ArrayList<>();

        this.root = new Node(new State(currentCity, availableTasks, currentTasks, vehicle.capacity()), null);

        /*List<Node> nodeList = new ArrayList<>();
        nodeList.add(root);

        for (ListIterator<Node> nodeIter = nodeList.listIterator(); nodeIter.hasNext(); ) {

            Node node = nodeIter.next();


        }*/

        Deque<Node> nodeQueue = new ArrayDeque<>();

        nodeQueue.addFirst(root);

        // Until the queue is not empty
        while (nodeQueue.size() > 0) {

            // Pop the node I want to work on
            Node currentNode = nodeQueue.pop();

            // Create all the children
            for (Action action : actionList) {

                State currentState = currentNode.getState();

                if (currentState.isActionPossible(action)) {

                    State childState = createState(currentNode.getState(), action);

                    Node child = currentNode.addChild(childState);

                    nodeQueue.push(child);
                }
            }

        }
    }

    /*public void createTree(State currentState) {

    }*/


    public State createState(State currentState, Action action) {

        List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
        List<Task> currentTasks = new ArrayList<>(currentState.getCurrentTasks());
        int availableCapacity = currentState.getAvailableCapacity();

        for (Task task : currentState.getCurrentTasks()) {

            if (task.deliveryCity.equals(action.getDestination())) {
                currentTasks.remove(task);
                //TODO: deliver logist
            }

        }

        switch (action.getActionType()) {

            case PICKUP_AND_MOVE:
                // Search if there is an available task from the city where the agent is
                // He picks it up if the vehicle has enough capacity available - it has to be already checked

                for (Task task : currentState.getAvailableTasks()) {


                    availableTasks.remove(task);
                    currentTasks.add(task);
                    availableCapacity -= task.weight;
                    //TODO: pickup and move logist
                    //return new State(action.getDestination(), availableTasks, currentTasks, availableCapacity);

                }

                break;

            case MOVE:
                // In case of a move action, the only thing that can change is the current tasks list that has already
                // been updated. So this case is useless
                //TODO: move logist
                //return new State(action.getDestination(), availableTasks, currentTasks, availableCapacity)
                break;
        }

            return new State(action.getDestination(), availableTasks, currentTasks, availableCapacity);

    }


    public void createActions() {

        //TODO: create actions

        this.actionList = null;
    }
}
