package main.java.ips;

import main.java.ips.ExtendedHofkinModel.Input;
import main.java.scenarios.DiscreteScenarioTree;

public class DHoffkinInput implements Input {
	private final int numTimePeriods;
	private final int maxAirborne;
	private final double groundCost;
	private final double airCost;
	private final DHoffkinFlightParser.DemandStruct demands;
	private final DiscreteScenarioTree tree;

	public DHoffkinInput(int maxAirborne,
			double groundCost, double airCost, DHoffkinFlightParser.DemandStruct demands,
			DiscreteScenarioTree tree) {
		super();
		this.numTimePeriods = demands.getNumTimePeriods();
		this.maxAirborne = maxAirborne;
		this.groundCost = groundCost;
		this.airCost = airCost;
		this.demands = demands;
		this.tree = tree;
	}

	@Override
	public int getNumTimePeriods() {
		return numTimePeriods;
	}

	@Override
	public double getGroundCost() {
		return groundCost;
	}

	@Override
	public double getAirCost() {
		return airCost;
	}

	@Override
	public int getMaxAirborne() {
		return maxAirborne;
	}

	@Override
	public double getScenProbability(int s) {
		return tree.getScenario(s).getProbability();
	}

	@Override
	public Iterable<Integer> getScenarios() {
		return tree.getScenarioIds();
	}

	@Override
	public Iterable<? extends Iterable<Integer>> getNodes(int i) {
		return tree.getScenarioNodes(i);
	}

	@Override
	public double getCapacity(int scenario, int timePeriod) {
		return tree.getScenario(scenario).getCapacity(timePeriod);
	}

	@Override
	public Iterable<Integer> getFlightDurations() {
		return demands.getDurations();
	}

	@Override
	public double getNumDeparting(int duration, int timePeriod) {
		return demands.getDemand(duration, timePeriod);
	}

	@Override
	public double getEnroute(int i) {
		return demands.getEnroute(i);
	}

}
