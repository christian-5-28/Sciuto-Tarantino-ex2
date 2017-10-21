package mainPack;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

/**
 * Created by lorenzotara on 20/10/17.
 */
public class DeliberativeStrategy {

    private List<State> goalStateList;
    private List<DeliberativeAction> actionList;

    public DeliberativeStrategy(Topology topology) {
        this.goalStateList = new ArrayList<>();

        createActions(topology);
    }

    public Plan astar(Vehicle vehicle, TaskSet tasks) {
        //createTreeRoot(vehicle, tasks);
        return null;
    }

    public Plan astar(Vehicle vehicle, TaskSet availabletasks, TaskSet currentTasks) {
        List<Task> carriedTasks = new ArrayList<>(currentTasks);
        //createTree(vehicle, availabletasks, carriedTasks);
        return null;
    }

    public Plan bfs(Vehicle vehicle, TaskSet tasks) {

        Deque<State> notVisitedQueue = new ArrayDeque<>();

        Set<State> visitedNodesSet = new HashSet<>();

        City currentCity = vehicle.getCurrentCity();
        List<Task> availableTasks = new ArrayList<>(tasks);
        List<Task> currentTasks = new ArrayList<>();
        List<Action> actionAlreadyExecuted = new ArrayList<>();

        State root = new State(currentCity, availableTasks, currentTasks, actionAlreadyExecuted, vehicle.capacity(), 0);

        notVisitedQueue.addFirst(root);

        while (!notVisitedQueue.isEmpty()){

            State currentState = notVisitedQueue.pop();

            if(isGoalState(currentState)){
                goalStateList.add(currentState);
            }

            for (State child : getChildren(currentState)) {

                if(visitedNodesSet.contains(child)){
                    continue;
                }
                if(!notVisitedQueue.contains(child)){
                    notVisitedQueue.push(child);
                }
                visitedNodesSet.add(currentState);
            }
        }

        return createOptimalPlan(goalStateList, currentCity);
    }

    private Plan createOptimalPlan(List<State> goalStateList, City startCity) {

        State bestGoalState = goalStateList.get(0);

        for (State state : goalStateList) {
            if(state.getDistanceCost() > bestGoalState.getDistanceCost()){
                bestGoalState = state;
            }
        }

        return new Plan(startCity, bestGoalState.getActionsAlreadyExecuted());

    }

    private boolean isGoalState(State currentState) {

        return currentState.getAvailableTasks().isEmpty()
                && currentState.getCurrentTasks().isEmpty();
    }


   /* public Plan bfs(Vehicle vehicle, TaskSet availabletasks, TaskSet currentTasks) {
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

            //createAllTheChildren(currentNode, nodeQueue);

        }
    }*/

    private List<State> getChildren(State currentState) {

        List<State> stateList = new ArrayList<>();

        // Create all the children
        for (DeliberativeAction deliberativeAction : actionList) {

            if (currentState.isActionPossible(deliberativeAction)) {

                State childState = createState(currentState, deliberativeAction);

                stateList.add(childState);
            }
        }

        return stateList;
    }


    public State createState(State currentState, DeliberativeAction deliberativeAction) {


        double distanceCost = currentState.getDistanceCost();
        List<Action> actionsExecuted = currentState.getActionsAlreadyExecuted();
        List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
        List<Task> currentTasks = new ArrayList<>(currentState.getCurrentTasks());
        int availableCapacity = currentState.getAvailableCapacity();

        for (Task task : currentState.getCurrentTasks()) {

            if (task.deliveryCity.equals(deliberativeAction.getDestination())) {

                //TODO: deliver logist
                actionsExecuted.add(new Action.Delivery(task));

                currentTasks.remove(task);
                availableCapacity += task.weight;
            }

        }


        if (deliberativeAction.isPickup()) {
            // Search if there is an available task from the city where the agent is
            // He picks it up if the vehicle has enough capacity available - it has to be already checked
            for (Task task : currentState.getAvailableTasks()) {
                if (task.pickupCity.equals(currentState.getCurrentCity())) {

                    availableTasks.remove(task);
                    currentTasks.add(task);
                    availableCapacity -= task.weight;

                    actionsExecuted.add(new Action.Pickup(task));
                }
            }
        }

        actionsExecuted.add(new Action.Move(deliberativeAction.getDestination()));

        return new State(deliberativeAction.getDestination(), availableTasks, currentTasks, actionsExecuted, availableCapacity, distanceCost);

    }


    public void createActions(Topology topology) {

        actionList = new ArrayList<>();

        for (City city : topology.cities()) {

            DeliberativeAction deliberativeAction1 = new DeliberativeAction(city, true);
            DeliberativeAction deliberativeAction2 = new DeliberativeAction(city, false);

            this.actionList.add(deliberativeAction1);
            this.actionList.add(deliberativeAction2);
        }

    }
}
