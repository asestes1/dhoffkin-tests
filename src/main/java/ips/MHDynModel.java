package ips;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.lang.reflect.Method;
import java.util.Iterator;

public class MHDynModel {
    public final static int UNLIMITED = -1;

    public interface Input {
        int getNumTimePeriods();

        double getGroundCost();

        double getAirCost();

        double getDivertCost();

        int getMaxAirborne();

        double getScenProbability(int s);

        Iterable<Integer> getScenarios();

        Iterable<? extends Iterable<Integer>> getNodes(int i);

        double getCapacity(int scenario, int timePeriod);

        Iterable<DiscreteFlight> getFlights();

        double getEnroute(int i);

    }

    public static GRBModel solveModel(Input input, GRBEnv myEnv, boolean verbose) throws GRBException {
        GRBModel myModel = setupModel(input, myEnv, verbose);
        myModel.optimize();
        return myModel;
    }

    public static GRBModel solveModel(Input input) throws GRBException {
        return solveModel(input, new GRBEnv(), false);
    }

    private static GRBModel setupModel(Input input, GRBEnv myEnv, boolean verbose) throws GRBException {
        GRBModel myModel = new GRBModel(myEnv);
        if (!verbose) {
            myModel.set(GRB.IntParam.OutputFlag, 0);
        }
        addVars(myModel, input);
        myModel.update();
        addConstraints(myModel, input);
        myModel.update();
        return myModel;
    }


    private static void addVars(GRBModel myModel, Input input) throws GRBException {
        addDepartVars(myModel, input);
        addAirVars(myModel, input);
        addDivertVars(myModel, input);
        addLandVars(myModel, input);
    }

    private static String getDepartVarName(int flightId, int timeIndex, int scenario) {
        return "DEP; FID: " + flightId + ", Time: " + timeIndex + ", Scen: " + scenario;
    }

    private static void addDepartVars(GRBModel model, Input input) throws GRBException {
        int numTimePeriods = input.getNumTimePeriods();
        double groundCost = input.getGroundCost();
        for (DiscreteFlight f : input.getFlights()) {
            int depIndex = f.getDepartTimePeriod();
            for (int j = depIndex; j < numTimePeriods + 1 - f.getFlightDuration(); j++) {
                for (int s : input.getScenarios()) {
                    double probability = input.getScenProbability(s);
                    model.addVar(0.0, 1.0, groundCost * (j - depIndex) * probability,
                            GRB.BINARY, getDepartVarName(f.getFlightId(), j, s));
                }
            }
        }
    }


    private static void addAirVars(GRBModel model, Input myInput)
            throws GRBException {
        double airCost = myInput.getAirCost();
        int numTimePeriods = myInput.getNumTimePeriods();
        double maxAirborne = myInput.getMaxAirborne();

        for (int s : myInput.getScenarios()) {
            double probability = myInput.getScenProbability(s);
            for (int i = 0; i < numTimePeriods; i++) {
                if (maxAirborne != UNLIMITED) {
                    model.addVar(0.0, maxAirborne, airCost * probability,
                            GRB.INTEGER, getAirVarName(s, i));
                } else {
                    model.addVar(0.0, GRB.INFINITY, airCost * probability,
                            GRB.INTEGER, getAirVarName(s, i));
                }
            }
        }
    }

    private static void addLandVars(GRBModel model, Input myInput) throws GRBException {
        int numTimePeriods = myInput.getNumTimePeriods();
        for (int s : myInput.getScenarios()) {
            for (int i = 0; i < numTimePeriods; i++) {
                model.addVar(0.0, myInput.getCapacity(s, i), 0.0,
                        GRB.INTEGER, getLandVarName(s, i));
            }
        }
    }

    private static void addDivertVars(GRBModel model, Input myInput) throws GRBException {
        double divertCost = myInput.getDivertCost();
        int numTimePeriods = myInput.getNumTimePeriods();
        for (int s : myInput.getScenarios()) {
            double probability = myInput.getScenProbability(s);
            for (int i = 0; i < numTimePeriods; i++) {
                model.addVar(0.0, GRB.INFINITY, divertCost * probability,
                        GRB.INTEGER, getDivertVarName(s, i));
            }
        }
    }


    public static String getAirVarName(int scenario, int timePeriod) {
        return "AIR: " + scenario + "," + timePeriod;
    }

    public static String getLandVarName(int scenario, int timePeriod) {
        return "LAND: " + scenario + "," + timePeriod;
    }

    public static String getDivertVarName(int scenario, int timePeriod) {
        return "DIVERT: " + scenario + "," + timePeriod;
    }

    public static String getDepartAAConstrName(int scenario1, int scenario2, int flightId, int t) {
        return "DEPART_AA: " + scenario1 + "," + scenario2 + "," + flightId + "," + t;
    }

    public static String getDepartureConstrName(int scenario, int flightId) {
        return "DEP_NODE: " + scenario + "," + flightId;
    }

    public static String getArrivalNodeConstrName(int scenario, int timePeriod) {
        return "ARR_NODE: " + scenario + "," + timePeriod;
    }

    private static void addConstraints(GRBModel model, Input input) throws GRBException {
        addDepartureConstraints(model, input);
        addArrivalNodeConstraints(model, input);
        addAntiAnticipatoryConstraints(model, input);
    }


    private static void addAntiAnticipatoryConstraints(GRBModel model,
                                                       Input input) throws GRBException {
        int numTimePeriods = input.getNumTimePeriods();
        for (DiscreteFlight f : input.getFlights()) {
            int flightId = f.getFlightId();
            for (int i = f.getDepartTimePeriod(); i < numTimePeriods + 1 - f.getFlightDuration(); i++) {
                for (Iterable<Integer> nodes : input.getNodes(i)) {
                    Iterator<Integer> scenIter = nodes.iterator();
                    int firstScen = scenIter.next();
                    while (scenIter.hasNext()) {
                        int nextScen = scenIter.next();
                        GRBVar firstDepartVar = model.getVarByName(getDepartVarName(flightId, i, firstScen));
                        GRBVar nextDepartVar = model.getVarByName(getDepartVarName(flightId, i, nextScen));
                        model.addConstr(firstDepartVar, GRB.EQUAL, nextDepartVar, getDepartAAConstrName(firstScen, nextScen, flightId, i));
                    }
                }
            }
        }
    }

    private static void addDepartureConstraints(GRBModel model, Input input) throws GRBException {
        int numTimePeriods = input.getNumTimePeriods();
        for (int s : input.getScenarios()) {
            for (DiscreteFlight f : input.getFlights()) {
                GRBLinExpr times = new GRBLinExpr();
                for (int t = f.getDepartTimePeriod(); t < numTimePeriods + 1 - f.getFlightDuration(); t++) {
                    times.addTerm(1.0, model.getVarByName(getDepartVarName(f.getFlightId(), t, s)));
                }
                model.addConstr(times, GRB.EQUAL, 1.0, getDepartureConstrName(f.getFlightId(), s));
            }
        }
    }

    private static void addArrivalNodeConstraints(GRBModel model, Input input) throws GRBException {
        int numTimePeriods = input.getNumTimePeriods();
        for (int s : input.getScenarios()) {
            for (int t = 0; t < numTimePeriods; t++) {
                GRBLinExpr inFlow = new GRBLinExpr();
                inFlow.addConstant(input.getEnroute(t));
                for (DiscreteFlight f : input.getFlights()) {
                    int dur = f.getFlightDuration();
                    if (t - dur >= f.getDepartTimePeriod()) {
                        inFlow.addTerm(1.0, model.getVarByName(getDepartVarName(f.getFlightId(), t - dur, s)));
                    }
                }
                if (t > 0) {
                    inFlow.addTerm(1.0, model.getVarByName(getAirVarName(s, t - 1)));
                }
                GRBLinExpr outFlow = new GRBLinExpr();
                outFlow.addTerm(1.0, model.getVarByName(getAirVarName(s, t)));
                outFlow.addTerm(1.0, model.getVarByName(getDivertVarName(s, t)));
                outFlow.addTerm(1.0, model.getVarByName(getLandVarName(s, t)));

                model.addConstr(inFlow, GRB.LESS_EQUAL, outFlow, getArrivalNodeConstrName(s, t));
            }
        }
    }

}
