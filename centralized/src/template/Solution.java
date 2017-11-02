package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Christian on 02/11/2017.
 */
public class Solution {

    private Map<Vehicle, TaskSet> vehicleTasksMap;
    private Map<Task, Integer> timePickUp;
    private Map<Task, Integer> timeDelivery;
    private Map<Task, Vehicle> vehicleMap;

    public Solution(){

        vehicleTasksMap = new HashMap<>();
        timePickUp = new HashMap<>();
        timeDelivery = new HashMap<>();
        vehicleMap = new HashMap<>();
    }

    /**
     * constraint on the fact that each task of the vehicle has a weight lower
     * then the capacity of the vehicle and also that the sum of the wieghts
     * is lower than the capacity
     * @param vehicle
     * @return
     */
    public boolean constraint1(Vehicle vehicle){

        int totalWeight = 0;
        for (Task task : vehicleTasksMap.get(vehicle)) {
            if(vehicle.capacity() < task.weight) //TODO: questo controllo è inutile, cioè termino prima l'esecuzione ma non mi sembra utile
                return false;
            totalWeight += task.weight;
        }

        return totalWeight < vehicle.capacity();
    }

    /**
     * this constraint checks if the time for the pickUp of a task
     * is strictly lower than the delivery time of that task
     * @param task
     * @return
     */
    public boolean constraint2(Task task){

        return timePickUp.get(task) < timeDelivery.get(task);

    }


    /**
     * checks if a task is linked with a vehicle, then
     * the task is inside the taskSet linked to that vehicle
     * @param task
     * @return
     */
    public boolean constraint3(Task task){

        Vehicle vehicle = vehicleMap.get(task);

        return vehicleTasksMap.get(vehicle).contains(task);

    }

    /**
     * alternative version
     */
    public boolean constraint3Alternative(Vehicle vehicle){

        for (Task task : vehicleTasksMap.get(vehicle)) {

            if(vehicleMap.get(task).id() != vehicle.id())
                return false;

        }

        return true;
    }

    //TODO: objective function
    public double objectiveFunction(){
        return 0.;
    }

}
