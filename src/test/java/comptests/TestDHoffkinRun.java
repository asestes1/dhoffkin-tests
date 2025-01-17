package comptests;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import ips.DHoffkinFlightParser;
import ips.DHoffkinInput;
import ips.ExtendedHofkinModel;
import ips.MHDynModel;
import ips.MHFlightParser;
import ips.MHInput;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;

import org.junit.Test;

import scenarios.DiscreteScenarioTree;
import scenarios.ScenarioTreeFactory;
import util.BTSParser;
import util.CapacityGetter;
import util.TimeZoneGetter;

public class TestDHoffkinRun {

    @Test
    public void testRunDHoffkin() throws IOException, GRBException {
        Duration disc = Duration.ofMinutes(5);
        System.out.println("Reading flights");
        File btsFile = new File(TestDHoffkinRun.class.getClassLoader().getResource("OnTime_2017_07_15.csv")
                .getFile());
        OffsetDateTime startTime = LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime endTime = LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(ZoneOffset.UTC);
        BTSParser.ResultStruct btsResults = BTSParser.separateForGDPPlanning(
                BTSParser.filterByAirportAndTimeRange(btsFile, startTime, endTime, "ORD"), startTime, endTime);

        int maxAir = 1000;

        System.out.println("Parsing flights to DH demands");
        DHoffkinFlightParser.DemandStruct demand = DHoffkinFlightParser.wrapBTSOutput(btsResults, disc);
        System.out.println(demand);

        System.out.println("Making scenario tree");
        DiscreteScenarioTree tree = ScenarioTreeFactory.makeLoToHigh(demand.getNumTimePeriods(), 0, 48, 3, 33, 12,
                false, 0);

        DHoffkinInput myInput = new DHoffkinInput(maxAir, 1.0, 2.0, 24, demand, tree);

        System.out.println("Building and Solving DH Model");
        ExtendedHofkinModel.solveModel(myInput, new GRBEnv(), true);

        System.out.println("Parsing flights to MH demands");
        MHFlightParser.DemandStruct demand2 = MHFlightParser.wrapBTSOutput(btsResults, disc);

        System.out.println("Building and Solving MH model");
        MHInput myInput2 = new MHInput(maxAir, 1.0, 2.0, 24.0, demand2, tree);
        MHDynModel.solveMhdModel(myInput2, new GRBEnv(), true);
    }

    @Test
    public void compTestsDHoffkin() throws IOException, GRBException, IllegalArgumentException {
        boolean append = false;
        boolean verbose = false;

//        String[] airports = {"ORD"};
//        Duration[] maxLengths = {Duration.ofHours(6)};
//        Duration[] discs = {Duration.ofMinutes(2)};
//        Integer[] param_cases = {4};
//        Double[] divertFactors = {1.0};
//        String[] airports = {"ATL", "ORD", "DFW", "LGA", "SFO", "DCA"};
        String[] airports = {"ATL", "DFW", "ORD", "LGA", "SFO", "DCA"};
        Duration[] maxLengths = {Duration.ofHours(2), Duration.ofHours(3), Duration.ofHours(4), Duration.ofHours(5), Duration.ofHours(6)};
        Duration[] discs = {Duration.ofMinutes(2), Duration.ofMinutes(5), Duration.ofMinutes(10), Duration.ofMinutes(15)};
        Integer[] param_cases = {1, 2, 3, 4, 5, 6};

        Double[] divertFactors = {1.0, 2.0, 1000.0};
        Duration padding = Duration.ofHours(3);
        LocalDateTime[] localStartTimes = {LocalDateTime.of(2017, 7, 15, 7, 0),
                LocalDateTime.of(2017, 7, 15, 17, 0)};

        double groundCost = 1.0;
        double airCost = 3.0;
        int lookahead = 0;
        boolean probAlt = false;

        FileWriter fw = new FileWriter("new_results.csv", append);
        BufferedWriter writer = new BufferedWriter(fw);
        if (!append) {
            writer.write(
                    "APT,VFR,IFR,WMAX,START,END,MAXLENGTH,NUM_SITTING,NUM_AIR,DISC,CASE,AIRCOST," +
                            "LOOKAHEAD,NUM_TIME_PERIODS,EARLY_CHANGE,LATE_CHANGE,PROB_ALT,DIVERT_FACTOR," +
                            "MH_SOLVETIME,MHD_SOLVETIME,DHD_SOLVETIME,MH_FEASIBLE,OBJ_MH,"+
                            "OBJ_DIVERT,MH_NODES,MHD_NODES,DHD_NODES,MHD_DIVERT,DHD_DIVERT,\n");
        }


        File btsFile = new File(TestDHoffkinRun.class.getClassLoader().getResource("OnTime_2017_07_15.csv")
                .getFile());
        GRBEnv myEnv = new GRBEnv();

        int counter = 0;
        for (String airport : airports) {
            ZoneId airportZoneId = TimeZoneGetter.getTimeZone(airport);
            OffsetDateTime earliestStart = localStartTimes[0].atZone(airportZoneId).toOffsetDateTime();
            OffsetDateTime latestEnd = localStartTimes[localStartTimes.length - 1]
                    .plus(maxLengths[maxLengths.length - 1]).plus(padding).atZone(airportZoneId).toOffsetDateTime();
            HashSet<BTSParser.FlightStruct> relevantFlights = BTSParser.filterByAirportAndTimeRange(btsFile,
                    earliestStart, latestEnd, airport);

            int vfr = CapacityGetter.getVfr(airport);
            int ifr = CapacityGetter.getIfr(airport);
            int wmax = vfr - ifr;

            for (LocalDateTime startTime : localStartTimes) {
                OffsetDateTime start = startTime.atZone(airportZoneId).toOffsetDateTime();
                for (Duration maxLength : maxLengths) {
                    System.out.println(airport + "," + startTime + "," + maxLength);
                    OffsetDateTime end = start.plus(maxLength).plus(padding);
                    BTSParser.ResultStruct separatedFlights = BTSParser.separateForGDPPlanning(relevantFlights, start,
                            end);

                    int numSitting = separatedFlights.getSittingFlights().size();
                    int numAir = separatedFlights.getAirborneFlights().size();
                    for (Duration disc : discs) {
                        int numTimePeriodsInHour = (int) (Duration.ofHours(1).toNanos() / disc.toNanos());
                        int numTimePeriods = (int) (Duration.between(start, end).toNanos() / disc.toNanos());
                        int earliestChange = 2 * numTimePeriodsInHour;
                        int latestChange = (int) (Duration.between(start, start.plus(maxLength)).toNanos()
                                / disc.toNanos());

                        MHFlightParser.DemandStruct myMHDemands = MHFlightParser.wrapBTSOutput(separatedFlights, disc);
                        DHoffkinFlightParser.DemandStruct myDHDemands = DHoffkinFlightParser
                                .wrapBTSOutput(separatedFlights, disc);

                        for (int param_case : param_cases) {
                            if (param_case == 2) {
                                wmax = ExtendedHofkinModel.UNLIMITED;
                            } else if (param_case == 3) {
                                airCost = 2.0;
                            } else if (param_case == 4) {
                                probAlt = true;
                            } else if (param_case == 5) {
                                wmax = 0;
                            } else if (param_case == 6) {
                                lookahead = numTimePeriodsInHour / 2;
                            }
                            for (double divertFactor : divertFactors) {
                                System.out.println(counter++);
                                double divertCost = divertFactor * numTimePeriodsInHour * airCost;

                                // Run experiment

                                DiscreteScenarioTree myTree = ScenarioTreeFactory.makeLoToHigh(numTimePeriods,
                                        earliestChange, latestChange, ifr, vfr, numTimePeriodsInHour, probAlt, lookahead);

                                MHDynModel.Input myMHInput = new MHInput(wmax, groundCost, airCost, divertCost,
                                        myMHDemands, myTree);
                                GRBModel mhModel = MHDynModel.solveMhModel(myMHInput, myEnv, verbose);
                                double solveTimeMH = mhModel.get(GRB.DoubleAttr.Runtime);
                                int statusMH = mhModel.get(GRB.IntAttr.Status);
                                double objectiveMH = Double.NaN;
                                boolean feasible = true;
                                if (statusMH == GRB.Status.OPTIMAL) {
                                    objectiveMH = mhModel.get(GRB.DoubleAttr.ObjVal);
                                } else {
                                    feasible = false;
                                }
                                double mhNodes = mhModel.get(GRB.DoubleAttr.NodeCount);
                                mhModel.dispose();

                                GRBModel mhdModel = MHDynModel.solveMhdModel(myMHInput, myEnv, verbose);
                                double solveTimeMHD = mhdModel.get(GRB.DoubleAttr.Runtime);
                                int statusMHD = mhdModel.get(GRB.IntAttr.Status);
                                double objectiveMHD = Double.NaN;
                                if (statusMHD == GRB.Status.OPTIMAL) {
                                    objectiveMHD = mhdModel.get(GRB.DoubleAttr.ObjVal);
                                }
                                double mhdNodes = mhdModel.get(GRB.DoubleAttr.NodeCount);
                                double mhdDivert = MHDynModel.getAverageDiversions(myMHInput, mhdModel);
                                mhdModel.dispose();

                                DHoffkinInput myDHInput = new DHoffkinInput(wmax, groundCost, airCost, divertCost, myDHDemands, myTree);
                                GRBModel dhModel = ExtendedHofkinModel.solveModel(myDHInput, myEnv, verbose);
                                double solveTimeDH = dhModel.get(GRB.DoubleAttr.Runtime);
                                int statusDH = dhModel.get(GRB.IntAttr.Status);
                                double objectiveDH = Double.NaN;
                                if (statusDH == GRB.Status.OPTIMAL) {
                                    objectiveDH = dhModel.get(GRB.DoubleAttr.ObjVal);
                                }
                                double dhNodes = dhModel.get(GRB.DoubleAttr.NodeCount);
                                double dhDivert = ExtendedHofkinModel.getAverageDiversions(myDHInput, dhModel);
                                dhModel.dispose();

                                if (statusMHD == GRB.Status.OPTIMAL && statusDH == GRB.Status.OPTIMAL) {
                                    if (objectiveDH > 0.5) {
                                        double percent_diff = Math.abs(objectiveDH - objectiveMHD) / objectiveDH;
                                        if (percent_diff > 0.001) {
                                            writer.close();
                                            throw new GRBException("Methods produce different objective values. MHD Obj: "
                                                    + objectiveMHD + ". DHD Obj: " + objectiveDH + ". Percent difference: "
                                                    + percent_diff);
                                        }
                                    } else {
                                        double abs_diff = Math.abs(objectiveDH - objectiveMHD);
                                        if (abs_diff > 0.001) {
                                            writer.close();
                                            throw new GRBException("Methods produce different objective values. MH Obj: "
                                                    + objectiveMH + ". DH Obj: " + objectiveDH + ". Absolute difference: "
                                                    + abs_diff);
                                        }
                                    }
                                } else {
                                    writer.close();
                                    throw new IllegalArgumentException("Invalid value in model statuses. Status of MHD: "
                                            + statusMH + ", status of DHD: " + statusDH + ".");
                                }

                                writer.write(airport + "," + vfr + "," + ifr + "," + wmax + "," + start + "," + end + ","
                                        + maxLength.toHours() + "," + numSitting + "," + numAir + "," + disc.toMinutes()
                                        + "," + param_case + "," + airCost + "," + lookahead + "," + numTimePeriods + ","
                                        + earliestChange + "," + latestChange + "," + probAlt + "," + divertFactor + ","
                                        + solveTimeMH + ","
                                        + solveTimeMHD + "," + solveTimeDH + "," + feasible + ","
                                        + objectiveMH + "," + objectiveDH + "," + mhNodes+","+mhdNodes + "," + dhNodes +
                                        "," + mhdDivert + "," + dhDivert + "," + "\n");
                            }
                            // Reset parameters
                            wmax = vfr - ifr;
                            airCost = 3.0;
                            lookahead = 0;
                            probAlt = false;
                        }
                    }
                }
            }
        }
        writer.close();
        return;
    }
}
