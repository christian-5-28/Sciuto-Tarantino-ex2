package mainPack;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

import java.util.Map;


/**
 * Created by lorenzotara on 03/10/17.
 */
public class ReactiveAgent implements ReactiveBehavior {

    private double discountFactor;

    private Agent myAgent;

    private Reinforcement reinforcement;


    /**
     * The setup method is called exactly once, before the simulation
     * starts and before any other method is called.
     *
     */
    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

        myAgent = agent;

        discountFactor = agent.readProperty("discount-factor", Double.class,
                0.95);

        reinforcement = new Reinforcement(topology, distribution, myAgent, discountFactor);

        reinforcement.valueIteration();

    }

    /**
     * This method is called every time the agent arrives in a new city and is not carrying a task.
     * The agent can see at most one available task in the city and has to decide whether or not to
     * accept the task. It is possible that there is no task in which case availableTask is null.
     *
     * 1)
     * If the agent decides to pick up the task, the platform will take over the control of the
     * vehicle and deliver the task on the shortest path. The next time this method is called the
     * vehicle will have dropped the task at its destination.
     *
     * 2)
     * If the agent decides to refuse the task, it chooses a neighboring city to move to.
     * A refused task disappears and will not be available the next time the agent visits the city.
     *
     */
    @Override
    public Action act(Vehicle vehicle, Task availableTask) {

        State currentState = reinforcement.getState(vehicle.getCurrentCity(), availableTask == null ? null : availableTask.deliveryCity);

        Topology.City bestNextCity = reinforcement.getNextBestCity(currentState);

        if(currentState.getTaskDestination() == bestNextCity){
            return new Action.Pickup(availableTask);
        }
        else {
           return new Action.Move(bestNextCity);
        }
    }


}
