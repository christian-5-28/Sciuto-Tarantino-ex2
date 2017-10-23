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

    class PathNode{

        private State parentState;
        private Deque<Action> actionExecuted;

        public PathNode(State parentState, Deque<Action> actionExecuted) {
            this.parentState = parentState;
            this.actionExecuted = actionExecuted;
        }
    }

    //private Map<State, PathNode> pathMap;
    private List<DeliberativeAction> actionList;
    private double costPerKm;

    public DeliberativeStrategy() {

        //this.pathMap = new HashMap<>();

        //createActions(topology);
    }

    public Plan astar(Vehicle vehicle, TaskSet tasks, List<Task> carriedTasks) {

        goalStateList = new ArrayList<>();

        LinkedList<State> openList = new LinkedList<>();

        Set<State> closedSet = new HashSet<>();

        //TODO: added
        this.costPerKm = vehicle.costPerKm();

        City currentCity = vehicle.getCurrentCity();
        List<Task> availableTasks = new ArrayList<>(tasks);
        List<Task> currentTasks = carriedTasks;
        List<Action> actionAlreadyExecuted = new ArrayList<>();

        State root = new State(currentCity, availableTasks, currentTasks, actionAlreadyExecuted, vehicle.capacity(), 0);

        openList.add(root);

        int iter = 0;

        //RIGHT ONE

        while (!openList.isEmpty()) {

            State current = openList.pop();

            /*System.out.println(iter);
            System.out.println("\n\nMossa scelta\n\n");*/
            System.out.println("H Cost: " + current.getHeuristic());
            /*System.out.println("Current city: " + current.getCurrentCity());
            System.out.println("Available tasks: " + current.getAvailableTasks());
            System.out.println("Current tasks: " + current.getCurrentTasks());*/
            System.out.println("\n");


            if (isGoalState(current)) {
                goalStateList.add(current);
                break;
            }

            //openList.remove(current);
            if (!closedSet.contains(current)) {
                closedSet.add(current);
            }

            for (State child : getAllChildren(current)) {

                if (closedSet.contains(child)) continue;

                if (!openList.contains(child)) {

                    openList.add(child);

                }

                double tentative_gScore = current.getDistanceCost() + current.getCurrentCity().distanceTo(child.getCurrentCity());


                if (closedSet.contains(child) && !compare(tentative_gScore, closedSet, child)) {
                    closedSet.remove(child);
                    closedSet.add(child);
                }

            }

            Collections.sort(openList, State.compareByTotalCost);
            iter++;

        }

        State finalState = goalStateList.get(0);
        return new Plan(currentCity, finalState.getActionsAlreadyExecuted());


    }

    private boolean compare(double tentative, Set<State> closedSet, State child) {

        for (State state : closedSet) {
            if (state.equals(child)) {
                return tentative >= state.getDistanceCost();
            }
        }

        return false;
    }



    /*private State findBestMove(List<State> openList) {

        State bestMove = null;
        double bestMoveCost = Double.POSITIVE_INFINITY;

        for (State possibleMove : openList) {

            double moveCost = calculateCost(possibleMove);

            // Finding the best possible move from current position
            if (moveCost < bestMoveCost) {
                bestMove = possibleMove;
                bestMoveCost = moveCost;
            }
        }

        return bestMove;
    }*/



    /*private double calculateCost(State bestMove) {

        double g = bestMove.getDistanceCost();
        double h = heuristic(bestMove);

        return g + h;
    }*/

    private void heuristic(State bestMove) {

        double avTaskMaxCost = 0;

        for (Task availableTask : bestMove.getAvailableTasks()) {
            double cost = availableTask.pathLength() + bestMove.getCurrentCity().distanceTo(availableTask.pickupCity);

            if (cost > avTaskMaxCost) {
                avTaskMaxCost = cost;
            }
        }

        double currTaskMaxCost = 0;

        for (Task currentTask : bestMove.getCurrentTasks()) {

            double cost = currentTask.pathLength();

            if (cost > currTaskMaxCost) {
                currTaskMaxCost = cost;
            }
        }

        bestMove.updateTotalCost(Math.max(currTaskMaxCost, avTaskMaxCost));

    }

    private void heuristic2(State bestMove) {

        List<Task> currentNotContained = new ArrayList<>();
        List<Task> availableNotContained = new ArrayList<>();

        extractNotContained(bestMove, currentNotContained, availableNotContained);

        double hCost = 0;

        for (Task task : currentNotContained) {
            hCost += task.pathLength();
        }

        for (Task task : availableNotContained) {
            hCost += task.pathLength();
        }

        bestMove.updateTotalCost(hCost);

    }

    private void extractNotContained(State bestMove, List<Task> currentNotContained, List<Task> availableNotContained) {

        List<Task> currentTasks = bestMove.getCurrentTasks();
        List<Task> availableTasks = bestMove.getAvailableTasks();

        compareTasks(currentTasks, availableTasks, currentNotContained);
        compareTasks(availableTasks, currentTasks, availableNotContained);

        /*for (Task currentTask : currentTasks) {

            List<City> path = currentTask.path();
            ArrayList<Task> otherCurrentTasks = new ArrayList<>(currentTasks);
            otherCurrentTasks.remove(currentTask);

            if (!citiesContained(path, availableTasks)
                    && !citiesContained(path, otherCurrentTasks)) {

                currentNotContained.add(currentTask);
            }

        }

        for (Task availableTask : availableTasks) {

            List<City> path = availableTask.path();
            ArrayList<Task> otherAvailableTasks = new ArrayList<>(availableTasks);
            otherAvailableTasks.remove(availableTask);

            if (!citiesContained(path, availableTasks)
                    && !citiesContained(path, otherAvailableTasks)) {

                availableNotContained.add(availableTask);
            }
        }*/

    }

    private void compareTasks(List<Task> tasksCompared, List<Task> tasksToCompare, List<Task> finalList) {

        for (Task taskCompared : tasksCompared) {

            List<City> path = taskCompared.path();
            ArrayList<Task> otherComparedTasks = new ArrayList<>(tasksCompared);
            otherComparedTasks.remove(taskCompared);

            if (!citiesContained(path, tasksToCompare)
                    && !citiesContained(path, otherComparedTasks)) {

                finalList.add(taskCompared);
            }

        }

    }

    private boolean citiesContained(List<City> path, List<Task> comparingTasks) {

        for (Task comparingTask : comparingTasks) {

            List<City> comparingPath = comparingTask.path();

            if (path.size() <= comparingPath.size()) {

                for (City city : path) {

                    if (comparingPath.contains(city)) {

                        return true;
                    }
                }
            }
        }

        return false;

    }

    /*public Plan astar(Vehicle vehicle, TaskSet availabletasks, TaskSet currentTasks) {
        List<Task> carriedTasks = new ArrayList<>(currentTasks);
        //createTree(vehicle, availabletasks, carriedTasks);
        return null;
    }*/

    public Plan bfs(Vehicle vehicle, TaskSet tasks, List<Task> carriedTasks){

        goalStateList = new ArrayList<>();

        Deque<State> notVisitedQueue = new ArrayDeque<>();

        Set<State> visitedNodesSet = new HashSet<>();

        City currentCity = vehicle.getCurrentCity();
        List<Task> availableTasks = new ArrayList<>(tasks);
        List<Task> currentTasks = new ArrayList<>(carriedTasks);
        List<Action> actionAlreadyExecuted = new ArrayList<>();

        if(currentCity.name.equals("Basel")){
            int i = 0;
        }

        State root = new State(currentCity, availableTasks, currentTasks, actionAlreadyExecuted, vehicle.capacity(), 0);

        notVisitedQueue.addFirst(root);

        int iter = 0;

        while (!notVisitedQueue.isEmpty()){

            State currentState = notVisitedQueue.pop();
            System.out.println("iter: "+ iter + " queueSize: " + notVisitedQueue.size());

            if(!visitedNodesSet.contains(currentState)){

                if(isGoalState(currentState)){

                    goalStateList.add(currentState);

                }

                else{

                    visitedNodesSet.add(currentState);
                    notVisitedQueue.addAll(getAllChildren(currentState));

                    /*for (Map.Entry<State, Deque<Action>> childStateActionEntry : getAllChildren(currentState).entrySet()) {

                        State childState = childStateActionEntry.getKey();
                        //if(visitedNodesSet.contains(childState))
                        //    continue;

                        pathMap.put(childState, new PathNode(currentState, childStateActionEntry.getValue()));
                        notVisitedQueue.add(childState);

                    }*/
                }

            }

            iter++;
        }

        return createOptimalPlan(goalStateList, currentCity);

    }

    public Plan bfs(Vehicle vehicle, TaskSet tasks) {

        return bfs(vehicle, tasks, new ArrayList<>());

    }


    private boolean isGoalState(State currentState) {

        return currentState.getAvailableTasks().isEmpty()
                && currentState.getCurrentTasks().isEmpty();
    }


    private Plan createOptimalPlan(List<State> goalStateList, City startCity) {

        State bestGoalState = goalStateList.get(0);

        for (State state : goalStateList) {
            if(state.getDistanceCost() < bestGoalState.getDistanceCost()){
                bestGoalState = state;
            }
        }

        return new Plan(startCity, bestGoalState.getActionsAlreadyExecuted());
        //return new Plan(startCity, createPath(pathMap, bestGoalState));

    }

    /*private List<Action> createPath(Map<State, PathNode> pathMap, State bestGoalState) {

        State tempState = bestGoalState;
        List<Action> actionList = new ArrayList<>();

        while (pathMap.containsKey(tempState)){

            PathNode tempNode = pathMap.get(tempState);

            for (Action action : tempNode.actionExecuted) {

                actionList.add(action);

            }

            tempState = tempNode.parentState;

        }

        Collections.reverse(actionList);

        return actionList;

    }*/

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

    /*Map<State, Deque<Action>> getAllChildren(State currentState){

        Map<State, Deque<Action>> childrenMap = new HashMap<>();

        for (City neighbourCity : currentState.getCurrentCity().neighbors()) {

            double distanceCost = currentState.getDistanceCost();
            Deque<Action> actionsExecuted = new ArrayDeque<>();
            List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
            List<Task> currentTasks = new ArrayList<>(currentState.getCurrentTasks());
            int availableCapacity = currentState.getAvailableCapacity();

            actionsExecuted.push(new Action.Move(neighbourCity));

            distanceCost += currentState.getCurrentCity().distanceTo(neighbourCity);

            for (Task taskTaken : currentState.getCurrentTasks()) {

                if (taskTaken.deliveryCity.equals(neighbourCity)) {

                    actionsExecuted.push(new Action.Delivery(taskTaken));

                    currentTasks.remove(taskTaken);

                    availableCapacity += taskTaken.weight;

                }

            }

            for (Task availableTask : currentState.getAvailableTasks()) {

                if (availableTask.pickupCity.equals(neighbourCity) && availableTask.weight <= availableCapacity) {

                    actionsExecuted.push(new Action.Pickup(availableTask));

                    availableTasks.remove(availableTask);

                    availableCapacity -= availableTask.weight;

                    currentTasks.add(availableTask);


                }

            }

            State childState = new State(neighbourCity, availableTasks, currentTasks, availableCapacity, distanceCost);

            childrenMap.put(childState, actionsExecuted);
        }

        return childrenMap;

    }*/

    List<State> getAllChildren(State currentState){

        List<State> childrenList = new ArrayList<>();

        for (City neighbourCity : currentState.getCurrentCity().neighbors()) {

            double distanceCost = currentState.getDistanceCost();
            List<Action> actionsExecuted = new ArrayList<>(currentState.getActionsAlreadyExecuted());
            List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
            List<Task> currentTasks = new ArrayList<>(currentState.getCurrentTasks());
            int availableCapacity = currentState.getAvailableCapacity();

            actionsExecuted.add(new Action.Move(neighbourCity));

            distanceCost += currentState.getCurrentCity().distanceTo(neighbourCity);

            for (Task taskTaken : currentState.getCurrentTasks()) {

                if(taskTaken.deliveryCity.equals(neighbourCity)){

                    actionsExecuted.add(new Action.Delivery(taskTaken));

                    currentTasks.remove(taskTaken);

                    availableCapacity += taskTaken.weight;

                }

            }

            for (Task availableTask : currentState.getAvailableTasks()) {

                if(availableTask.pickupCity.equals(neighbourCity) && availableTask.weight <= availableCapacity){

                    actionsExecuted.add(new Action.Pickup(availableTask));

                    availableTasks.remove(availableTask);

                    availableCapacity -= availableTask.weight;

                    currentTasks.add(availableTask);



                }

            }

            State childState = new State(neighbourCity, availableTasks, currentTasks, actionsExecuted, availableCapacity, distanceCost);

            heuristic(childState);

            childrenList.add(childState);

        }

        return childrenList;

    }

    /*
    private List<State> getDeliveryPickUpChildren(State currentState) {

        List<State> childrenList = new ArrayList<>();

        double distanceCost = currentState.getDistanceCost();
        List<Action> actionsExecuted = new ArrayList<>(currentState.getActionsAlreadyExecuted());
        List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
        List<Task> currentTasks = new ArrayList<>(currentState.getCurrentTasks());
        int availableCapacity = currentState.getAvailableCapacity();

        /**
         *
         * for each deliveryCity we check if there is an available task in that city that can be taken after we have
         * delivered the task, therefore we add all the Move actions to arrive at the deliveryCity, the Delivery Action,
         * we update the currentTasks list, the distance cost, the availableCapacity. Then, we add the pickUp action
         * and finally we update the availableCapacity.
         *

        for (Task currentTask : currentTasks) {

            for (Task availableTask : availableTasks) {

                if(currentTask.deliveryCity.equals(availableTask.pickupCity) && availableTask.weight <= availableCapacity + currentTask.weight){

                    for (City city : currentState.getCurrentCity().pathTo(currentTask.deliveryCity)) {

                        actionsExecuted.add(new Action.Move(city));

                    }

                    actionsExecuted.add(new Action.Delivery(currentTask));

                    currentTasks.remove(currentTask);

                    distanceCost += currentState.getCurrentCity().distanceTo(currentTask.deliveryCity);

                    availableCapacity += currentTask.weight;

                    actionsExecuted.add(new Action.Pickup(availableTask));

                    availableTasks.remove(availableTask);

                    availableCapacity -= availableTask.weight;

                    State childState = new State(currentTask.deliveryCity, availableTasks, currentTasks, actionsExecuted, availableCapacity, distanceCost);

                    childrenList.add(childState);
                }

                return childrenList;
            }
        }

    }*/

    private List<State> getDeliveryChildren(State currentState) {

        List<State> childrenList = new ArrayList<>();

        for (Task currentTask : currentState.getCurrentTasks()) {

            double distanceCost = currentState.getDistanceCost();
            List<Action> actionsExecuted = new ArrayList<>(currentState.getActionsAlreadyExecuted());
            List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
            List<Task> currentTasks = new ArrayList<>(currentState.getCurrentTasks());
            int availableCapacity = currentState.getAvailableCapacity();

            for (City city : currentState.getCurrentCity().pathTo(currentTask.deliveryCity)) {

                actionsExecuted.add(new Action.Move(city));

            }

            actionsExecuted.add(new Action.Delivery(currentTask));

            currentTasks.remove(currentTask);

            distanceCost += currentState.getCurrentCity().distanceTo(currentTask.deliveryCity);

            availableCapacity += currentTask.weight;

            /**
             * here we check if there is an available task in the delivery city that can be taken only after we
             * have delivered the task.

            for (Task availableTask : availableTasks) {

                if (currentTask.deliveryCity.equals(availableTask.pickupCity)
                        && availableTask.weight > currentState.getAvailableCapacity()
                        && availableTask.weight <= availableCapacity ) {

                    actionsExecuted.add(new Action.Pickup(availableTask));
                    availableTasks.remove(availableTask);
                    availableCapacity -= availableTask.weight;

                    /**
                     * We assume that only one task can be in the delivery city

                    break;
                }
            } */

            State childState = new State(currentTask.deliveryCity, availableTasks, currentTasks, actionsExecuted, availableCapacity, distanceCost);

            childrenList.add(childState);

        }

        return childrenList;
    }

    private List<State> getPickUpChildren(State currentState) {

        List<State> childrenList = new ArrayList<>();

        for (Task availableTask : currentState.getAvailableTasks()) {

            double distanceCost = currentState.getDistanceCost();
            List<Action> actionsExecuted = new ArrayList<>(currentState.getActionsAlreadyExecuted());
            List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
            List<Task> currentTasks = new ArrayList<>(currentState.getCurrentTasks());
            int availableCapacity = currentState.getAvailableCapacity();


            /**
             * for each task that can be taken cosnsidering its weight we add to the actionExecuted List
             * all the Move action in order to arrive in the pickUp city and the PickupAction. then, we
             * update the distanceCost, the availableTask and currentTask lists
             */
            if(availableTask.weight <= currentState.getAvailableCapacity()){

                for (City city : currentState.getCurrentCity().pathTo(availableTask.pickupCity)) {

                    actionsExecuted.add(new Action.Move(city));

                }

                actionsExecuted.add(new Action.Pickup(availableTask));

                distanceCost += currentState.getCurrentCity().distanceTo(availableTask.pickupCity);
                availableCapacity -= availableTask.weight;

                availableTasks.remove(availableTask);
                currentTasks.add(availableTask);

                /**
                 * Finally, we create the new child state and we add it to the childrenList
                 */
                State childState = new State(availableTask.pickupCity, availableTasks, currentTasks, actionsExecuted, availableCapacity, distanceCost);
                childrenList.add(childState);
            }

        }

        return childrenList;

    }


    /*public State createState(State currentState, DeliberativeAction deliberativeAction) {


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

    }*/
}
