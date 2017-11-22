package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private long timeout_bid;

	private AuctionStrategy auctionStrategy;
	private CompanyStrategy companyStrategy;
	private Solution bestSolution;
	private long timeout_plan;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();


		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_auction.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the plan method cannot execute more than timeout_bid milliseconds
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		System.out.println("BID:");
		System.out.println(timeout_bid);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		System.out.println("PLAN:");
		System.out.println(timeout_plan);

		this.auctionStrategy = new AuctionStrategy(distribution, timeout_bid, this.agent, this.topology);


		System.out.println("Setup..");

	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {

		auctionStrategy.auctionCompleted(previous, bids, winner);

		/*if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
		}*/
	}
	
	@Override
	public Long askPrice(Task task) {

		System.out.println("Ask price...");


		return (long)auctionStrategy.makeBid(task);

		/*if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;

		return (long) Math.round(bid);*/
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		if(tasks.isEmpty()){
			return createEmptyPlan(vehicles);
		}

		else {

			companyStrategy = new CompanyStrategy(tasks, vehicles);
			System.out.println("starting SLS");
			long start = System.currentTimeMillis();
			bestSolution = companyStrategy.SLS(5000, timeout_bid, 0.35, 50, companyStrategy.initialSolution());
			long end = System.currentTimeMillis();
			System.out.println("completed SLS in " + (end - start) / 1000 + "seconds");

			List<Plan> planList = new ArrayList<>();

			for (Vehicle vehicle : vehicles) {
				planList.add(createVehiclePlan(vehicle, bestSolution.getVehicleActionMap().get(vehicle)));
			}

			System.out.println("Plan...");

			return planList;


		}

//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		/*Plan planVehicle1 = naivePlan(vehicle, tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;*/
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




	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
}
