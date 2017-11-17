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

/**
 * CentralizedCompany class:
 * it implements the centralized behavior
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

    /**
     * it uses the SLS method in order to obtain a best solution for all the task
     */
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        if(tasks.isEmpty()){
            return createEmptyPlan(vehicles);
        }

        else{

            companyStrategy = new CompanyStrategy(tasks, vehicles);
            System.out.println("starting SLS");
            long start = System.currentTimeMillis();
            bestSolution = companyStrategy.SLS(5000, timeout_plan, 0.35, 50, companyStrategy.initialSolution());
            long end = System.currentTimeMillis();
            System.out.println("completed SLS in " + (end - start)/1000 + "seconds");

            List<Plan> planList = new ArrayList<>();

            for (Vehicle vehicle : vehicles) {
                planList.add(createVehiclePlan(vehicle, bestSolution.getVehicleActionMap().get(vehicle)));
            }

            return planList;
        }


    }

    private List<Plan> createEmptyPlan(List<Vehicle> vehicles) {

        List<Plan> planList = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {

            planList.add(new Plan(vehicle.homeCity()));
            
        }

        return planList;
    }

    /**
     * it creates the plan for the vehicle by emulating its path using the actionList
     * of the vehicle.
     */
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


}
