package mainPack;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.*;


public class DeliberativeStrategy {

    private double vehicleCost;

    public DeliberativeStrategy(double vehicleCost) {

        this.vehicleCost = vehicleCost;

    }

    /**
     * We implemented a Comparator in order to use it for the PriorityQueue for the openSet
     * in the A* algorithm
     */
    private class CostComparator implements Comparator<Node>{

        @Override
        public int compare(Node o1, Node o2) {
            if(o1.getFinalCost() < o2.getFinalCost())
                return -1;

            if(o1.getFinalCost() > o2.getFinalCost())
                return 1;

            return 0;
        }
    }

    /**
     * Implementation of the BFS algorithm, the Node class is used in order to store the parent
     * relation and the costs of the current node
     *
     * @param carriedTasks: list of task that the vehicle might carry at the beginning of the
     *                    computation due to an invalid plan in a multi-agent situation
     * @return
     */
    public Plan bfs(Vehicle vehicle, TaskSet tasks, List<Task> carriedTasks){

        Deque<Node> notVisitedQueue = new ArrayDeque<>();
        Set<Node> visitedNodesSet = new HashSet<>();

        City currentCity = vehicle.getCurrentCity();
        List<Task> availableTasks = new ArrayList<>(tasks);
        List<Task> currentTasks = new ArrayList<>(carriedTasks);

        Node root = new Node(new State(currentCity, availableTasks, currentTasks, vehicle.capacity()), null, 0);

        notVisitedQueue.addFirst(root);

        int iter = 0;

        Node bestGoalNode = null;

        while (!notVisitedQueue.isEmpty()){

            Node currentNode = notVisitedQueue.poll();

            if(!visitedNodesSet.contains(currentNode)){

                if(isGoalState(currentNode.getState())
                        && (bestGoalNode == null || currentNode.getDistanceCost() < bestGoalNode.getDistanceCost())){

                    bestGoalNode = currentNode;
                }

                else{
                    visitedNodesSet.add(currentNode);
                    notVisitedQueue.addAll(getAllNodeChildren(currentNode, false));
                }
            }
            iter++;
        }

        System.out.println("number of iterations: " + iter);

        System.out.println(bestGoalNode.getDistanceCost());

        return new Plan(currentCity, createPath(bestGoalNode));
    }


    private boolean isGoalState(State currentState) {

        return currentState.getAvailableTasks().isEmpty()
                && currentState.getCarriedTasks().isEmpty();
    }

    /**
     * This method creates all the next possible states for the current state.
     * For each neighbour city we check if there are task that we can deliver there, then
     * we check if we can pick up available tasks in the neighbour city considering
     * the available capacity of the vehicle.
     * @return
     */
    private List<Node> getAllNodeChildren(Node currentNode, boolean isAstar) {

        List<Node> nodeList = new ArrayList<>();
        State currentState = currentNode.getState();

        for (City neighbourCity : currentState.getCurrentCity().neighbors()) {

            double distanceCost = currentNode.getDistanceCost();
            List<Task> availableTasks = new ArrayList<>(currentState.getAvailableTasks());
            List<Task> currentTasks = new ArrayList<>(currentState.getCarriedTasks());
            int availableCapacity = currentState.getAvailableCapacity();

            distanceCost += currentState.getCurrentCity().distanceTo(neighbourCity)*vehicleCost;

            for (Task taskTaken : currentState.getCarriedTasks()) {

                if(taskTaken.deliveryCity.equals(neighbourCity)){

                    currentTasks.remove(taskTaken);
                    availableCapacity += taskTaken.weight;
                }
            }

            for (Task availableTask : currentState.getAvailableTasks()) {

                if(availableTask.pickupCity.equals(neighbourCity) && availableTask.weight <= availableCapacity){

                    availableTasks.remove(availableTask);
                    availableCapacity -= availableTask.weight;
                    currentTasks.add(availableTask);
                }
            }

            State childState = new State(neighbourCity, availableTasks, currentTasks, availableCapacity);
            Node nodeChild = new Node(childState, currentNode, distanceCost);

            if(isAstar){
                nodeChild.updateCosts(heuristic(childState));
            }

            nodeList.add(nodeChild);
        }
        return nodeList;
    }



    public Plan astar(Vehicle vehicle, TaskSet tasks, List<Task> currentTasks) {

        Queue<Node> notVisitedQueue = new PriorityQueue<>( new CostComparator());

        List<Node> nodesVisitedList = new ArrayList<>();

        City currentCity = vehicle.getCurrentCity();
        List<Task> availableTasks = new ArrayList<>(tasks);
        List<Task> carriedTasks = new ArrayList<>(currentTasks);
        State root = new State(currentCity, availableTasks, carriedTasks, vehicle.capacity());

        notVisitedQueue.add(new Node(root, null, heuristic(root)));

        List<Action> actionList = new ArrayList<>();

        int iter = 0;

        while (!notVisitedQueue.isEmpty()){

            Node currentNode = notVisitedQueue.poll();
            State currentState = currentNode.getState();

            if(isGoalState(currentState)){

                actionList = createPath(currentNode);
                break;
            }

            if(!nodesVisitedList.contains(currentNode)){

                nodesVisitedList.add(currentNode);

                for (Node childNode : getAllNodeChildren(currentNode, true)) {

                    /**
                     * if the nodeVisitedList contains a node with the same state of the childNode but the distanceCost
                     * of the childNode is less from the distanceCost of the node in the nodeVisitedList, we remove
                     * the node from the nodeVisitedList, in order to visit the new better node.
                     */
                    if(nodesVisitedList.contains(childNode) && childNode.getDistanceCost() < nodesVisitedList.get(nodesVisitedList.indexOf(childNode)).getDistanceCost()){
                        nodesVisitedList.remove(childNode);
                    }

                    if(!notVisitedQueue.contains(childNode)){

                        notVisitedQueue.add(childNode);
                    }
                }
            }
            iter++;
        }

        System.out.println("number of iterations: " + iter);

        return new Plan(currentCity, actionList);

    }

    /**
     * Returns the maximum DistanceCost among all the avaiableTasks and the carriedTasks
     */
    private Double heuristic(State currenState) {

        double maxCarriedCost = 0.;
        for (Task task : currenState.getCarriedTasks()) {

            double cost = currenState.getCurrentCity().distanceTo(task.deliveryCity)*vehicleCost;

            if( cost > maxCarriedCost)
                maxCarriedCost = cost;
        }

        double maxAvailableCost = 0.;
        for (Task task : currenState.getAvailableTasks()) {

            double cost = (currenState.getCurrentCity().distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity))*vehicleCost;

            if( cost > maxAvailableCost)
                maxCarriedCost = cost;
        }


        return Math.max(maxAvailableCost, maxCarriedCost);
    }

    /**
     * this method returns the the list of actions done up to the currentNode.
     */
    private List<Action> createPath(Node currentNode) {

        LinkedList<State> path = new LinkedList<>();
        List<Action> actionList = new ArrayList<>();

        for(Node p = currentNode; p != null; p = p.getFather()){
            path.addFirst(p.getState());
        }

        for(int index = 0; index < path.size() - 1; index++){

            State currentState = path.get(index);
            State nextState = path.get(index + 1);

            actionList.addAll(createActions(currentState, nextState));
        }

        return actionList;
    }

    /**
     * this method return the actions for the transition from currentState to the nextState.
     */
    private List<Action> createActions(State currentState, State nextState) {

        List<Action> actionList = new ArrayList<>();
        actionList.add(new Action.Move(nextState.getCurrentCity()));
        int availableCapacity = currentState.getAvailableCapacity();

        /**
         *
         * for each task in the carriedTask we check if we can deliver a task in the nextCity, if so,
         * we add a new Delivery action to the actionList
         *
         */
        for (Task task : currentState.getCarriedTasks()) {

            if(task.deliveryCity.equals(nextState.getCurrentCity())){
                actionList.add(new Action.Delivery(task));
                availableCapacity += task.weight;
            }
        }

        /**
         *
         * for each task in the availableTask we check if we can pick-up a task in the nextCity, if so,
         * we add a new Pick-Up action to the actionList
         *
         */
        for (Task availableTask : currentState.getAvailableTasks()) {

            if (availableTask.pickupCity.equals(nextState.getCurrentCity()) && availableTask.weight <= availableCapacity) {

                actionList.add(new Action.Pickup(availableTask));
                availableCapacity -= availableTask.weight;
            }
        }

        return actionList;
    }


    private void extractNotContained(State bestMove, List<Task> currentNotContained, List<Task> availableNotContained) {

        List<Task> currentTasks = bestMove.getCarriedTasks();
        List<Task> availableTasks = bestMove.getAvailableTasks();

        compareTasks(currentTasks, availableTasks, currentNotContained);
        compareTasks(availableTasks, currentTasks, availableNotContained);
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

            for (City city : path) {

                if (comparingPath.contains(city)) {

                    return true;
                }
            }
        }

        return false;

    }

    private double heuristic2(State bestMove) {

        List<Task> currentNotContained = new ArrayList<>();
        List<Task> availableNotContained = new ArrayList<>();

        extractNotContained(bestMove, currentNotContained, availableNotContained);

        double hCost = 0;

        for (Task task : currentNotContained) {
            hCost += task.pathLength()*vehicleCost;
        }

        for (Task task : availableNotContained) {
            hCost += task.pathLength()*vehicleCost;
        }

        return hCost;

    }

}
