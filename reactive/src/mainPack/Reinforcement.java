package mainPack;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

/**
 * Created by lorenzotara on 05/10/17.
 */
public class Reinforcement {

    private double discountFactor;

    private final double STOPPING_CRITERION = 1e-6;

    private Map<State, Double> accumulatedValues;
    private Map<State, Action> best;

    private Topology topology;
    private TaskDistribution taskDistribution;
    private Agent agent;

    private List<State> stateList = new ArrayList<>();
    private List<Action> actionList = new ArrayList<>();

    public Reinforcement(Topology topology, TaskDistribution taskDistribution, Agent agent, double discountFactor) {

        this.topology = topology;
        this.taskDistribution = taskDistribution;
        this.agent = agent;
        this.discountFactor = discountFactor;
        this.accumulatedValues = new HashMap<>();
        this.best = new HashMap<>();

        initializeStates();
        createActions();

    }


    /**
     * Cartesian product of the cities in order to create the states.
     * There are no states with the same city as starting point and destination
     * and every combination can have the presence of a task or not.
     */
    private void initializeStates() {

        List<City> cities = topology.cities();

        for (City city0 : cities) {

            for (City city1 : cities) {

                if (city0.id != city1.id) {
                    stateList.add(new State(city0, city1, true));
                    stateList.add(new State(city0, city1, false));
                }
            }
        }
    }


    /**
     * Creation of the list of actions that consider every city and
     * every action type.
     */
    private void createActions() {

        List<City> cities = topology.cities();

        for (City city : cities) {

            for (Action.ActionType actionType : Action.ActionType.values()) {

                actionList.add(new Action(actionType, city));
            }
        }
    }


    private void initializeAccumalatedValues() {

        for (State state : stateList) {
            accumulatedValues.put(state, 0.);
        }
    }

    /**
     * Returns the reward the agent gets when it takes the action "actionSelected" from the state "currentState"
     * @param currentState
     * @param actionSelected
     * @return
     */
    private double reward(State currentState, Action actionSelected){

        City startingCity = currentState.getDestination();
        City destinationCity = actionSelected.getDestination();

        return taskDistribution
                .reward(startingCity, destinationCity)
                - startingCity.distanceTo(destinationCity) * agent.vehicles().get(0).costPerKm();

    }

    private double transitionFunction(State currentState, Action action, State nextState) {

        City actionDestinationCity = action.getDestination();
        City currentStateDestinationCity = currentState.getDestination();
        City nextStateStartingCity = nextState.getStartingCity();
        City nextStateDestinationCity = nextState.getDestination();

        if (currentStateDestinationCity != nextStateStartingCity
                || actionDestinationCity != nextStateDestinationCity) {
            return 0.;
        }

        switch (action.getActionType()) {

            case PICKUP:
                if(nextState.isTaskPresent()){
                    return taskDistribution.probability(currentStateDestinationCity, nextStateDestinationCity);
                }
                return 0.;

            case MOVE:
                if(!nextState.isTaskPresent() && nextStateDestinationCity.hasNeighbor(currentStateDestinationCity)) {
                    return taskDistribution.probability(currentStateDestinationCity, null);
                }
                return 0;

            default:
                return 0;
        }

    }


    private double sumOfWeightedValues(State state0, Action action) {

        double accumulatorQ = 0;

        for (State state1 : stateList) {
            accumulatorQ += transitionFunction(state0, action, state1) * accumulatedValues.get(state1);
        }

        return accumulatorQ;
    }


    private double functionQ(State state0, Action action) {

        return reward(state0, action) + discountFactor * sumOfWeightedValues(state0, action);
    }


    public void MDP() {

        class Tuple {

            State state;
            Action action;

            public Tuple(State state, Action action) {
                this.state = state;
                this.action = action;
            }
        }

        HashMap<Tuple, Double> tableQ = new HashMap<>();

        initializeAccumalatedValues();

        //Optional<Double> currentMaxValue = accumulatedValues.values().stream().reduce((x, y) -> Math.max(x,y));

        double currentV = 0;
        double maxQ = 0.;

        do {

            for (State state : stateList) {

                currentV = accumulatedValues.get(state);

                ArrayList<Double> valuesList = new ArrayList<>();

                for (Action action : actionList) {

                    Tuple stateActionTuple = new Tuple(state, action);

                    double q = functionQ(state, action);
                    valuesList.add(q);
                    tableQ.put(stateActionTuple, q);
                }

                maxQ = (valuesList.stream().reduce((x, y) -> Math.max(x,y))).get();

                accumulatedValues.put(state, maxQ);
            }


        }

        while (Math.abs(currentV - maxQ) <= STOPPING_CRITERION);
    }




    public Map<State, Double> getAccumulatedValues() {
        return new HashMap<>(accumulatedValues);
        //TODO: vedere se funziona la copia
    }

    public Map<State, Action> getBest() {
        return new HashMap<>(best);
        //TODO: vedere se funziona la copia
    }
}
