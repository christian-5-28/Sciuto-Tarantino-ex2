package mainPack;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
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
        createTreeRoot(vehicle, tasks);
        return null;
    }

    public Plan astar(Vehicle vehicle, TaskSet availabletasks, TaskSet currentTasks) {
        List<Task> carriedTasks = new ArrayList<>(currentTasks);
        createTree(vehicle, availabletasks, carriedTasks);
        return null;
    }

    public Plan bfs(Vehicle vehicle, TaskSet tasks) {
        createTreeRoot(vehicle, tasks);
        return null;
    }

    public Plan bfs(Vehicle vehicle, TaskSet availabletasks, TaskSet currentTasks) {
        List<Task> carriedTasks = new ArrayList<>(currentTasks);
        createTree(vehicle, availabletasks, carriedTasks);
        return null;
    }

    public void createTreeRoot(Vehicle vehicle, TaskSet tasks) {
        List<Task> currentTasks = new ArrayList<>(tasks);
        createTree(vehicle, tasks, currentTasks);
    }

    public void createTree(Vehicle vehicle, TaskSet freeTasks, List<Task> carriedTasks) {

        City currentCity = vehicle.getCurrentCity();
        List<Task> availableTasks = new ArrayList<>(freeTasks);
        List<Task> currentTasks = new ArrayList<>(carriedTasks);

        this.root = new Node(new State(currentCity, availableTasks, currentTasks, vehicle.capacity()), null);

        Deque<Node> nodeQueue = new ArrayDeque<>();

        nodeQueue.addFirst(root);

        // Until the queue is not empty
        while (!nodeQueue.isEmpty()) {

            // Pop the node I want to work on
            Node currentNode = nodeQueue.pop();

            createAllTheChildren(currentNode, nodeQueue);



        }
    }

    private void createAllTheChildren(Node currentNode, Deque<Node> nodeQueue) {

        State currentState = currentNode.getState();

        // Create all the children
        for (Action action : actionList) {

            if (currentState.isActionPossible(action)) {

                State childState = createState(currentNode.getState(), action);

                Node child = currentNode.addChild(childState);

                nodeQueue.push(child);
            }
        }
    }


    public State createState(State currentState, Action action) {

        List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
        List<Task> currentTasks = new ArrayList<>(currentState.getCurrentTasks());
        int availableCapacity = currentState.getAvailableCapacity();

        for (Task task : currentState.getCurrentTasks()) {

            if (task.deliveryCity.equals(action.getDestination())) {
                currentTasks.remove(task);
                availableCapacity += task.weight;
                //TODO: deliver logist
            }

        }


        if (action.isPickup()) {
            // Search if there is an available task from the city where the agent is
            // He picks it up if the vehicle has enough capacity available - it has to be already checked
            for (Task task : currentState.getAvailableTasks()) {
                if (task.pickupCity.equals(currentState.getCurrentCity())) {

                    availableTasks.remove(task);
                    currentTasks.add(task);
                    availableCapacity -= task.weight;

                    //TODO: pickup and move logist
                }
            }
        }

        else {
            // In case of a move action, the only thing that can change is the current tasks list that has already
            // been updated. So this case is useless
            //TODO: move logist
        }

        return new State(action.getDestination(), availableTasks, currentTasks, availableCapacity);

    }


    public void createActions() {

        //TODO: create actions

        this.actionList = null;
    }
}
