package oldAuctionAgent;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.*;

/**
 * Created by lorenzotara on 18/11/17.
 */
public class AgentStatus {

    // Map that contains a City that is a pickup city as a key and all the bids for that city as value
    private Map<City, List<Double>> pickUpCitiesBids;
    private Map<City, List<Double>> deliveryCitiesBids;

    // Map that contains a City that is a pickup city as a key and all the predictions for that city as value
    private Map<City, List<Double>> pickUpCitiesPredictions;
    private Map<City, List<Double>> deliveryCitiesPredictions;

    private Set<Task> tasksWon;
    private double averageBid = 0;
    private List<Double> errors;
    private boolean alreadyBid = false;

    /*public AgentStatus(TaskSet taskSet) {
        this.pickUpCitiesBids = new HashMap<>();
        this.deliveryCitiesBids = new HashMap<>();
        pickUpCitiesPredictions = new HashMap<>();
        deliveryCitiesPredictions = new HashMap<>();
        this.tasksWon = taskSet;
        this.errors = new ArrayList<>();

    }*/

    public AgentStatus() {
        this.pickUpCitiesBids = new HashMap<>();
        this.deliveryCitiesBids = new HashMap<>();
        pickUpCitiesPredictions = new HashMap<>();
        deliveryCitiesPredictions = new HashMap<>();
        this.tasksWon = new HashSet<>();
        this.errors = new ArrayList<>();

    }

    public Map<City, List<Double>> getPickUpCitiesPredictions() {
        return pickUpCitiesPredictions;
    }

    public Map<City, List<Double>> getDeliveryCitiesPredictions() {
        return deliveryCitiesPredictions;
    }

    public Map<City, List<Double>> getPickUpCitiesBids() {
        return pickUpCitiesBids;
    }

    public Map<City, List<Double>> getDeliveryCitiesBids() {
        return deliveryCitiesBids;
    }

    public Set<Task> getTasksWon() {

        Set<Task> taskSet = new HashSet<>();

        for (Task task : tasksWon) {
            taskSet.add(task);
        }

        return taskSet;
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

    public double getAverageError() {

        OptionalDouble averageError = errors.stream().mapToDouble(error -> error).average();

        if (averageError.isPresent()) {
            return averageError.getAsDouble();
        }

        return 0;

    }

    public void setAlreadyBid(boolean alreadyBid) {
        this.alreadyBid = alreadyBid;
    }
}
