package ips;

import scenarios.DiscreteScenarioTree;

public class MHInput implements MHDynModel.Input {
    private final int numTimePeriods;
    private final int maxAirborne;
    private final double groundCost;
    private final double airCost;
    private final double divertCost;
    private final MHFlightParser.DemandStruct demands;
    private final DiscreteScenarioTree tree;

    public MHInput(int maxAirborne,
                   double groundCost, double airCost, double divertCost,
                   MHFlightParser.DemandStruct demands,
                   DiscreteScenarioTree tree) {
        super();
        this.numTimePeriods = demands.getNumTimePeriods();
        this.maxAirborne = maxAirborne;
        this.groundCost = groundCost;
        this.airCost = airCost;
        this.divertCost = divertCost;
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
    public double getDivertCost() {
        return divertCost;
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
    public double getEnroute(int i) {
        return demands.getEnroute(i);
    }

    @Override
    public Iterable<DiscreteFlight> getFlights() {
        return demands.getFlights();
    }

}
