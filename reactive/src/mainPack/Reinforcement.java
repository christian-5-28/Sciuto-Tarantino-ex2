package mainPack;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class Reinforcement {

    private double discountFactor;

    private final double STOPPING_CRITERION = 1e-8;

    private Map<State, Double> accumulatedValuesMap;
    private Map<State, Action> bestActionPerStateMap;

    private Map<State, Map<Action, Double>> mapQ;

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
        this.accumulatedValuesMap = new HashMap<>();
        this.bestActionPerStateMap = new HashMap<>();
        this.mapQ = new HashMap<>();

        initializeStates();
        createActions();

        initializeMapQ();

    }


    /**
     * Cartesian product of the cities in order to create the states.
     * There are no states with the same city as starting point and destination,
     *
     */
    private void initializeStates() {

        List<City> cities = topology.cities();

        for (City city0 : cities) {

            stateList.add(new State(city0, null));

            for (City city1 : cities) {

                if (city0.id != city1.id) {
                    stateList.add(new State(city0, city1));
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

            actionList.add(new Action(city));
        }
    }


    private void initializeAccumulatedValues() {

        for (State state : stateList) {
            accumulatedValuesMap.put(state, 1.);
        }
    }

    /**
     * Returns the reward the agent gets when it takes the action "actionSelected" from the state "currentState",
     * if the possible destination city of the current state is equal to the next city of the action then the reward
     * is the expected reward from the taskDistribution minus the cost of the street, otherwise the reward is
     * the cost of the street
     */
    private double reward(State currentState, Action actionSelected){

        City currentCity = currentState.getCurrentCity();
        City possibleDestinationCity = currentState.getTaskDestination();
        City actionDestinationCity = actionSelected.getNextCity();

        double streetCost = currentCity.distanceTo(actionDestinationCity) * agent.vehicles().get(0).costPerKm();

        if(possibleDestinationCity != null && possibleDestinationCity.id == actionDestinationCity.id){

        return taskDistribution
                .reward(currentCity, actionDestinationCity) - streetCost;

        }

        else {

            return -streetCost;
        }

    }

    /**
     *
     * returns the probability to be in the specific nextState considering the currentState and the action selected.
     * If the city of the action selected is not equal to the currentState possibleDestination and it is not equal
     * to the nextState currentCity the probability is zero.
     *
     */
    public double transitionFunction(State currentState, Action action, State nextState) {

        City actionNextCity = action.getNextCity();
        City nextStateCurrentCity = nextState.getCurrentCity();

        for (Action validAction : currentState.getValidActionList()) {

            if(actionNextCity.id == validAction.getNextCity().id && actionNextCity.id == nextStateCurrentCity.id){

                return taskDistribution.probability(actionNextCity, nextState.getTaskDestination());
            }
        }

        return 0.;
    }


    private double sumOfWeightedValues(State state0, Action action) {

        double accumulatorQ = 0.;

        for (State state1 : stateList) {
            accumulatorQ += transitionFunction(state0, action, state1) * accumulatedValuesMap.get(state1);
        }
        return accumulatorQ;
    }


    private double functionQ(State state0, Action action) {

       return reward(state0, action) + discountFactor * sumOfWeightedValues(state0, action);

    }

    public void valueIteration(){

        initializeAccumulatedValues();

        while(true){

            Map<State, Double> tolerateErrorMap = new HashMap<>();

            for (State state : stateList) {

                double currentV = accumulatedValuesMap.get(state);
                double maxReward = Double.NEGATIVE_INFINITY;

                for (Action action : actionList) {

                    if(state.actionIsValid(action)){

                        double expectedRewardQ = functionQ(state, action);
                        mapQ.get(state).put(action, expectedRewardQ);

                        if(expectedRewardQ > maxReward){

                            maxReward = expectedRewardQ;
                            accumulatedValuesMap.put(state,maxReward);
                            bestActionPerStateMap.put(state, action);
                        }

                    }

                }

                tolerateErrorMap.put(state, Math.abs(currentV - accumulatedValuesMap.get(state)));
            }

            if(stoppingCriterionIsVerified(tolerateErrorMap)){
                return;
            }
        }
    }

    private boolean stoppingCriterionIsVerified(Map<State, Double> tolerateErrorMap) {

        int counter = 0;

        for (State state : stateList) {
            if(tolerateErrorMap.get(state) < STOPPING_CRITERION){
                counter++;
            }
        }

        return counter == stateList.size();
    }

    private void initializeMapQ(){

        for (State state : stateList) {
            for (Action action : state.getValidActionList()) {
                mapQ.put(state, new HashMap<>());
                mapQ.get(state).put(action, 0.);
            }
        }
    }



    public State getState(City currentCity, City possibleDestination){

        for (State state : stateList) {

            if(currentCity.equals(state.getCurrentCity()) &&
                    (possibleDestination == state.getTaskDestination() || possibleDestination.equals(state.getTaskDestination()))){

                return state;

            }
        }

        throw new IllegalArgumentException("state not found");
    }


    public City getNextBestCity(State state) {

        Action action = bestActionPerStateMap.get(state);
        return action.getNextCity();
    }

    public City getWorstCity(State state){

       double minValue = Collections.min(mapQ.get(state).values());

        for (Map.Entry<Action, Double> actionDoubleEntry : mapQ.get(state).entrySet()) {

            if(actionDoubleEntry.getValue().equals(minValue)){
                return actionDoubleEntry.getKey().getNextCity();
            }
        }

        throw new IllegalArgumentException("error: no worst action found");

    }
}
