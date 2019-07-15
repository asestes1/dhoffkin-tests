package ips;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.BTSParser;
import util.BTSParser.FlightStruct;
import util.DiscretizerUtil;

public final class MHFlightParser {
    private MHFlightParser() {

    }

    public static class DemandStruct {
        // This maps a flight duration to a list of demands. The length of each
        // list should be the number of time periods.
        private final HashSet<DiscreteFlight> flights;

        // enroute[i] should store the number enroute in each time period.
        private final List<Integer> enroute;

        private final int numTimePeriods;

        public DemandStruct(Collection<DiscreteFlight> flights,
                            List<Integer> enroute) {
            super();
            this.flights = new HashSet<DiscreteFlight>(flights);
            this.enroute = new ArrayList<Integer>(enroute);
            this.numTimePeriods = enroute.size();
        }

        public Set<DiscreteFlight> getFlights() {
            return new HashSet<DiscreteFlight>(flights);
        }

        public int getEnroute(int timePeriod) {
            return enroute.get(timePeriod);
        }

        public int getNumTimePeriods() {
            return numTimePeriods;
        }

        @Override
        public String toString() {
            StringBuilder mybuilder = new StringBuilder();
            mybuilder.append("Flights: \n");
            for (DiscreteFlight f : flights) {
                mybuilder.append("\t" + f.toString() + "\n");
            }
            mybuilder.append("Enroute: \n");
            mybuilder.append("\t [");
            for (Integer demand : enroute) {
                mybuilder.append(demand + ", ");
            }
            mybuilder.deleteCharAt(mybuilder.length()-1);
            mybuilder.deleteCharAt(mybuilder.length()-1);
            mybuilder.append("]\n");
            return mybuilder.toString();

        }
    }

    public static DemandStruct wrapBTSOutput(BTSParser.ResultStruct btsResults,
                                             Duration discretization) {
        OffsetDateTime startTime = btsResults.getStartTime();
        OffsetDateTime endTime = btsResults.getEndTime();
        int numTimePeriods = DiscretizerUtil.getNumTimePeriods(startTime,
                endTime, discretization);

        HashSet<DiscreteFlight> demandMap = wrapSittingFlights(
                btsResults.getSittingFlights(), numTimePeriods, startTime,
                discretization);

        ArrayList<Integer> enroute = wrapEnrouteFlights(btsResults.getAirborneFlights(), numTimePeriods, startTime, discretization);
        return new DemandStruct(demandMap, enroute);
    }

    public static ArrayList<Integer> wrapEnrouteFlights(
            Set<FlightStruct> airborneFlights, int numTimePeriods,
            OffsetDateTime startTime, Duration discretization) {
        return DHoffkinFlightParser.wrapEnrouteFlights(airborneFlights, numTimePeriods, startTime, discretization);
    }

    public static HashSet<DiscreteFlight> wrapSittingFlights(
            Iterable<BTSParser.FlightStruct> sittingFlights,
            int numTimePeriods, OffsetDateTime startTime,
            Duration discretization) {
        HashSet<DiscreteFlight> flights = new HashSet<DiscreteFlight>();

        for (BTSParser.FlightStruct f : sittingFlights) {
            int flightDur = DiscretizerUtil.durationToTimePeriods(
                    f.getFlightDuration(), discretization);
            int depIndex = DiscretizerUtil.timeToIndex(f.getDepartureTime(),
                    startTime, discretization);
            flights.add(new DiscreteFlight(f.getFlightId(), depIndex, flightDur));
        }
        return flights;
    }

}
