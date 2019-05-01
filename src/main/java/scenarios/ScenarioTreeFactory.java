package main.java.scenarios;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class ScenarioTreeFactory {
	private ScenarioTreeFactory() {

	}

	/**
	 * 
	 * @param numTimePeriods
	 *            - the number of time periods in the planning horizon
	 * @param earliestChange
	 *            - the earliest time period in which the capacity might change
	 *            (inclusive)
	 * @param latestChange
	 *            - the latest time period in which the capacity might change
	 *            (exclusive)
	 * @param low
	 *            - the low capacity of the airport
	 * @param high
	 *            - the high capacity
	 * @return
	 */
	public static DiscreteScenarioTree makeLoToHigh(int numTimePeriods,
			int earliestChange, int latestChange, int low, int high,
			int timePeriodsPerHour, boolean altProbs, int lookahead) {
		// Now we make the scenarios
		Set<DiscreteScenario> scenarios = new HashSet<DiscreteScenario>();
		int numScenarios = latestChange - earliestChange;

		for (int i = earliestChange; i < latestChange; i++) {
			ArrayList<Integer> capacities = new ArrayList<Integer>();
			double sum = 0;
			for (int j = 0; j < numTimePeriods; j++) {
				int currentRate = low;
				int timeIndexInHour = j % timePeriodsPerHour;

				if (j >= i) {
					currentRate = high;
					timeIndexInHour = (j-i) % timePeriodsPerHour;
				}

				if (timeIndexInHour == 0) {
					sum = 0;
				}

				int nextCapacity = (int) Math.floor((timeIndexInHour + 1.0)
						* currentRate / timePeriodsPerHour - sum);
				sum += nextCapacity;
				capacities.add(nextCapacity);
			}

			double probability = 1.0 / numScenarios;
			if (altProbs) {
				if (i < timePeriodsPerHour+earliestChange) {
					probability = 0.4 / timePeriodsPerHour;
				} else {
					probability = 0.6/ (numScenarios - timePeriodsPerHour);
				}
			}
			scenarios.add(new DiscreteScenario(i, probability, capacities));
		}
		return new DiscreteScenarioTree(numTimePeriods, scenarios, lookahead);

	}

}
