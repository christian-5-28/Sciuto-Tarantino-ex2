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

    private final double STOPPING_CRITERION = 1e-8;

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


    private void initializeAccumalatedValues() {

        for (State state : stateList) {
            accumulatedValues.put(state, 1.);
        }
    }

    /**
     * Returns the reward the agent gets when it takes the action "actionSelected" from the state "currentState",
     * if the possible destination city of the current state is equal to the next city of the action then the reward
     * is the expected reward from the taskDistribution minus the cost of the street, otherwise the reward is
     * the cost of the street
     */
    private double reward(State currentState, Action actionSelected){

        City currentCity = currentState.getStartingCity();
        City possibleDestinationCity = currentState.getDestination();
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

        //TODO: devi trattare quando lo stato attuale e prossimo sono uguali, ritorna zero

        City actionNextCity = action.getNextCity();
        City nextStateCurrentCity = nextState.getStartingCity();

        for (Action validAction : currentState.getValidActionList()) {

            if(actionNextCity.id == validAction.getNextCity().id && actionNextCity.id == nextStateCurrentCity.id){

                return taskDistribution.probability(actionNextCity, nextState.getDestination());
            }
        }

        return 0.;
    }


    private double sumOfWeightedValues(State state0, Action action) {

        double accumulatorQ = 0.;

        for (State state1 : stateList) {

            accumulatorQ += transitionFunction(state0, action, state1) * accumulatedValues.get(state1);
        }
        return accumulatorQ;
    }


    private double functionQ(State state0, Action action) {

       return reward(state0, action) + discountFactor * sumOfWeightedValues(state0, action);

    }

    public void valueIteration(){

        initializeAccumalatedValues();

        while(true){

            Map<State, Double> tolerateErrorList = new HashMap<>();

            for (State state : stateList) {

                double currentV = accumulatedValues.get(state);
                double maxReward = Double.NEGATIVE_INFINITY;

                for (Action action : actionList) {

                    if(state.actionIsValid(action)){

                        double expectedRewardQ = functionQ(state, action);

                        if(expectedRewardQ > maxReward){

                            maxReward = expectedRewardQ;
                            accumulatedValues.put(state,maxReward);
                            best.put(state, action);
                        }

                    }

                }

                tolerateErrorList.put(state, Math.abs(currentV - accumulatedValues.get(state)));

            }

            int counter = 0;

            for (State state : stateList) {

                if(tolerateErrorList.get(state) < STOPPING_CRITERION){

                    counter++;
                }
            }

            if(counter == stateList.size()){
                return;
            }
        }
    }







    /*public void valueIteration() {

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

        double currentV = 0.;
        double maxQ = 0.;
        Action bestAction = null;

        do {

            for (State state : stateList) {

                currentV = accumulatedValues.get(state);
                ArrayList<Double> valuesList = new ArrayList<>();

                for (Action action : actionList) {

                    Tuple stateActionTuple = new Tuple(state, action);

                    for (Action validAction : state.getValidActionList()) {

                        if(action.getNextCity().id == validAction.getNextCity().id){

                            double q = functionQ(state, action);
                            valuesList.add(q);

                            if (q > maxQ) {

                                maxQ = q;
                                bestAction = action;

                                if(bestAction == null){ //TODO: rimuovi codice debug

                                    int i = 0;
                                }
                            }

                            tableQ.put(stateActionTuple, q); //TODO: controllare se serve
                        }

                    }

                }

                accumulatedValues.put(state, maxQ);
                best.put(state, bestAction);
            }
        }
        while (Math.abs(currentV - maxQ) <= STOPPING_CRITERION);
    }*/


    public State getState(City currentCity, City possibleDestination){

        for (State state : stateList) {

            if(currentCity.equals(state.getStartingCity()) && (possibleDestination == state.getDestination() || possibleDestination.equals(state.getDestination()))){

                return state;

            }
        }

        throw new IllegalArgumentException("state not found");
    }


    public Map<State, Double> getAccumulatedValues() {
        return new HashMap<>(accumulatedValues);
        //TODO: vedere se funziona la copia
    }

    public Map<State, Action> getBest(){
        return best;
    }

    public City getNextBestCity(State state) {

        Action action = best.get(state);

        return action.getNextCity();
        //TODO: vedere se funziona la copia
    }
}
