package template;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Christian on 03/11/2017.
 */
public class CentralizedCompany implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private CompanyStrategy companyStrategy;
    private Solution bestSolution;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;


    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        companyStrategy = new CompanyStrategy(tasks, vehicles);
        System.out.println("starting SLS");
        bestSolution = companyStrategy.SLS(50000, 0.3);
        System.out.println("completed SLS");

        List<Plan> planList = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            planList.add(createVehiclePlan(vehicle, bestSolution.getVehicleActionMap().get(vehicle)));
        }


//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        /*Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        List<Plan> plans = new ArrayList<>();
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in "+duration+" milliseconds.");*/

        return planList;
    }

    private Plan createVehiclePlan(Vehicle vehicle, List<Action> actionList) {

        Plan plan = new Plan(vehicle.getCurrentCity());

        Topology.City currentCity = vehicle.getCurrentCity();

        for (Action action : actionList) {

            Task task = action.getTask();

            switch (action.getActionType()){
                case PICKUP:
                    if(action.getTask().pickupCity.equals(currentCity)){
                        plan.appendPickup(task);
                    }
                    else{
                        for (Topology.City city : currentCity.pathTo(task.pickupCity)) {
                            plan.appendMove(city);
                        }
                        plan.appendPickup(task);
                        currentCity = task.pickupCity;
                    }
                    break;
                case DELIVERY:
                    if(action.getTask().deliveryCity.equals(currentCity)){
                        plan.appendDelivery(task);
                    }
                    else{
                        for (Topology.City city : currentCity.pathTo(task.deliveryCity)) {
                            plan.appendMove(city);
                        }
                        plan.appendDelivery(task);
                        currentCity = task.deliveryCity;
                    }
                    break;
                default:
                    break;

            }

        }

        return plan;
    }


    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        Topology.City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (Topology.City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (Topology.City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }}
