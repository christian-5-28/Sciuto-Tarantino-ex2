package mainPack;

import logist.plan.Action;
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

    private List<State> goalStateList;
    private List<DeliberativeAction> actionList;
    private double costPerKm;

    public DeliberativeStrategy() {
        this.goalStateList = new ArrayList<>();

        //createActions(topology);
    }

    public Plan astar(Vehicle vehicle, TaskSet tasks) {

        goalStateList = new ArrayList<>();

        LinkedList<State> openList = new LinkedList<>();

        Set<State> closedSet = new HashSet<>();

        //TODO: added
        this.costPerKm = vehicle.costPerKm();

        City currentCity = vehicle.getCurrentCity();
        List<Task> availableTasks = new ArrayList<>(tasks);
        List<Task> currentTasks = new ArrayList<>();
        List<Action> actionAlreadyExecuted = new ArrayList<>();

        State root = new State(currentCity, availableTasks, currentTasks, actionAlreadyExecuted, vehicle.capacity(), 0);

        openList.add(root);

        int iter = 0;

        /// Almost RIGHT ONE

        while (!openList.isEmpty()) {

            State current = openList.pop();

            System.out.println(iter);

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





        /* boolean targetFound = false;
        while (true) {

            State nextMove = findBestMove(openList);

            System.out.println("Mossa scelta\n\n");
            System.out.println(nextMove.getCurrentCity());
            System.out.println(nextMove.getAvailableTasks());
            System.out.println(nextMove.getCurrentTasks());
            System.out.println("\n");

            targetFound = isGoalState(nextMove);

            if (targetFound) {
                goalStateList.add(nextMove);
                break;
            }

            openList.remove(nextMove);

            closedList.add(nextMove);

            List<State> nextMoveChildren = getAllChildren(nextMove);

            System.out.println("\n\nFigli\n\n");
            for (State nextMoveChild : nextMoveChildren) {
                System.out.println(nextMoveChild.getCurrentCity());
                System.out.println(nextMoveChild.getAvailableTasks());
                System.out.println(nextMoveChild.getCurrentTasks());
                System.out.println("\n");
            }

            // Every child is walkable
            // No child can be already in the openList
            openList.addAll(nextMoveChildren);

        }*/



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

    private double heuristic(State bestMove) {

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

        return Math.max(currTaskMaxCost, avTaskMaxCost);

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
                }

            }

            iter++;
        }

            /*if(isGoalState(currentState)){
                goalStateList.add(currentState);
            }


            for (State child : getAllChildren(currentState)) {

                if(visitedNodesSet.contains(child)){
                    continue;
                }
                if(!notVisitedQueue.contains(child)){
                    notVisitedQueue.push(child);
                }

            }
            visitedNodesSet.add(currentState);
        }*/

        return createOptimalPlan(goalStateList, currentCity);
    }

    private Plan createOptimalPlan(List<State> goalStateList, City startCity) {

        State bestGoalState = goalStateList.get(0);

        for (State state : goalStateList) {
            if(state.getDistanceCost() < bestGoalState.getDistanceCost()){
                bestGoalState = state;
            }
        }

        return new Plan(startCity, bestGoalState.getActionsAlreadyExecuted());

    }

    private boolean isGoalState(State currentState) {

        return currentState.getAvailableTasks().isEmpty()
                && currentState.getCurrentTasks().isEmpty();
    }



    public List<State> getAllChildren(State currentState){

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
            //TODO: togliere
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

/*
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

            */
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

                    */
/**
                     * We assume that only one task can be in the delivery city

                    break;
                }
            } *//*


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


            */
/**
             * for each task that can be taken cosnsidering its weight we add to the actionExecuted List
             * all the Move action in order to arrive in the pickUp city and the PickupAction. then, we
             * update the distanceCost, the availableTask and currentTask lists
             *//*

            if(availableTask.weight <= currentState.getAvailableCapacity()){

                for (City city : currentState.getCurrentCity().pathTo(availableTask.pickupCity)) {

                    actionsExecuted.add(new Action.Move(city));

                }

                actionsExecuted.add(new Action.Pickup(availableTask));

                distanceCost += currentState.getCurrentCity().distanceTo(availableTask.pickupCity);
                availableCapacity -= availableTask.weight;

                availableTasks.remove(availableTask);
                currentTasks.add(availableTask);

                */
/**
                 * Finally, we create the new child state and we add it to the childrenList
                 *//*

                State childState = new State(availableTask.pickupCity, availableTasks, currentTasks, actionsExecuted, availableCapacity, distanceCost);
                childrenList.add(childState);
            }

        }

        return childrenList;

    }
*/


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
