package main.java.ips;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import main.java.util.BTSParser;
import main.java.util.BTSParser.FlightStruct;
import main.java.util.DiscretizerUtil;

public final class DHoffkinFlightParser {
	private DHoffkinFlightParser() {

	}

	public static class DemandStruct {
		// This maps a flight duration to a list of demands. The length of each
		// list should be the number of time periods.
		private final HashMap<Integer, ArrayList<Integer>> demandMap;

		// enroute[i] should store the number enroute in each time period.
		private final List<Integer> enroute;
		
		private final int numTimePeriods;

		public DemandStruct(Map<Integer, ? extends List<Integer>> demandMap,
				List<Integer> enroute) {
			super();
			this.demandMap = new HashMap<Integer, ArrayList<Integer>>();
			for (Entry<Integer, ? extends List<Integer>> demandEntry : demandMap
					.entrySet()) {
				this.demandMap.put(demandEntry.getKey(),
						new ArrayList<Integer>(demandEntry.getValue()));
			}
			this.enroute = new ArrayList<Integer>(enroute);
			this.numTimePeriods =enroute.size();
		}

		public int getDemand(int flightDuration, int timePeriod) {
			return demandMap.get(flightDuration).get(timePeriod);
		}

		public int getEnroute(int timePeriod) {
			return enroute.get(timePeriod);
		}
		
		public int getNumTimePeriods(){
			return numTimePeriods;
		}

		public Set<Integer> getDurations(){
			return new HashSet<Integer>(demandMap.keySet());
		}
		@Override
		public String toString() {
			String myString = "Demand by duration: \n";
			for (Entry<Integer, ArrayList<Integer>> entry : demandMap
					.entrySet()) {
				myString += "\t" + entry.getKey() + ": [";
				for (Integer demand : entry.getValue()) {
					myString += demand + ", ";
				}
				myString = myString.substring(0, myString.length() - 2);
				myString += "]\n";
			}
			myString += "Enroute: \n";
			myString += "\t [";
			for (Integer demand : enroute) {
				myString += demand + ", ";
			}
			myString = myString.substring(0, myString.length() - 2);
			myString += "]\n";
			return myString;

		}
	}

	public static DemandStruct wrapBTSOutput(BTSParser.ResultStruct btsResults,
			Duration discretization) {
		OffsetDateTime startTime = btsResults.getStartTime();
		OffsetDateTime endTime = btsResults.getEndTime();
		int numTimePeriods = DiscretizerUtil.getNumTimePeriods(startTime,
				endTime, discretization);

		HashMap<Integer, ArrayList<Integer>> demandMap = wrapSittingFlights(
				btsResults.getSittingFlights(), numTimePeriods, startTime,
				discretization);
		
		ArrayList<Integer> enroute = wrapEnrouteFlights(btsResults.getAirborneFlights(),numTimePeriods,startTime,discretization);
		return new DemandStruct(demandMap, enroute);
	}

	public static ArrayList<Integer> wrapEnrouteFlights(
			Set<FlightStruct> airborneFlights, int numTimePeriods,
			OffsetDateTime startTime, Duration discretization) {
		ArrayList<Integer> enroute = new ArrayList<Integer>(numTimePeriods);
		for(int i=0; i < numTimePeriods; i++){
			enroute.add(0);
		}
		
		for (BTSParser.FlightStruct f : airborneFlights) {
			int arrIndex = DiscretizerUtil.timeToIndex(f.getArrivalTime(),
					startTime, discretization);
			enroute.set(arrIndex, enroute.get(arrIndex)+1);
		}	
		return enroute;
	}

	public static HashMap<Integer, ArrayList<Integer>> wrapSittingFlights(
			Iterable<BTSParser.FlightStruct> sittingFlights,
			int numTimePeriods, OffsetDateTime startTime,
			Duration discretization) {
		HashMap<Integer, ArrayList<Integer>> demandMap = new HashMap<Integer, ArrayList<Integer>>();

		for (BTSParser.FlightStruct f : sittingFlights) {
			int flightDur = DiscretizerUtil.durationToTimePeriods(
					f.getFlightDuration(), discretization);
			if (!demandMap.containsKey(flightDur)) {
				initializeDurationCategory(flightDur, numTimePeriods, demandMap);
			}
			int depIndex = DiscretizerUtil.timeToIndex(f.getDepartureTime(),
					startTime, discretization);
			ArrayList<Integer> demandsForThisDuration = demandMap
					.get(flightDur);
			demandsForThisDuration.set(depIndex,
					demandsForThisDuration.get(depIndex) + 1);
		}
		return demandMap;
	}

	public static void initializeDurationCategory(int flightDur,
			int numTimePeriods, HashMap<Integer, ArrayList<Integer>> demandMap) {
		ArrayList<Integer> list = new ArrayList<Integer>(numTimePeriods);
		for (int i = 0; i < numTimePeriods; i++) {
			list.add(0);
		}
		demandMap.put(flightDur, list);
	}

}
