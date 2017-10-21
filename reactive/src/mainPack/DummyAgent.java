package mainPack;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

/**
 * Created by Christian on 10/10/2017.
 */
public class DummyAgent implements ReactiveBehavior{

    private double discountFactor;

    private Agent myAgent;

    private Reinforcement reinforcement;


    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

        myAgent = agent;

        discountFactor = agent.readProperty("discount-factor", Double.class,
                0.95);

        reinforcement = new Reinforcement(topology, distribution, myAgent, discountFactor);

        reinforcement.valueIteration();
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {

        State currentState = reinforcement.getState(vehicle.getCurrentCity(), availableTask == null ? null : availableTask.deliveryCity);
        Topology.City bestNextCity = reinforcement.getWorstCity(currentState);
        if(currentState.getTaskDestination() == bestNextCity){
            return new Action.Pickup(availableTask);
        }
        else {
            return new Action.Move(bestNextCity);
        }

    }
}
