package template;

import logist.task.Task;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lorenzotara on 18/11/17.
 */
public class AgentStatus {

    // Map that contains a City that is a pickup city as a key and all the bids for that city as value
    private Map<City, List<Double>> pickUpCitiesBids;
    private Map<City, List<Double>> deliveryCitiesBids;
    private List<Task> tasksWon;
    private double averageBid = 0;
    private List<Double> errors;
    private boolean alreadyBid = false;

    public AgentStatus() {
        this.pickUpCitiesBids = new HashMap<>();
        this.deliveryCitiesBids = new HashMap<>();
        this.tasksWon = new ArrayList<>();
        this.errors = new ArrayList<>();

    }

    public Map<City, List<Double>> getPickUpCitiesBids() {
        return pickUpCitiesBids;
    }

    public Map<City, List<Double>> getDeliveryCitiesBids() {
        return deliveryCitiesBids;
    }

    public List<Task> getTasksWon() {
        return tasksWon;
    }

    public List<Double> getErrors() {
        return errors;
    }

    public double getAverageBid() {
        return averageBid;
    }

    public boolean hasAlreadyBid() {
        return alreadyBid;
    }
}
