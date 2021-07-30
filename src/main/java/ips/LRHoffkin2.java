package ips;

import java.util.Iterator;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public final class LRHoffkin2 {
	public final static int UNLIMITED = -1;

	private LRHoffkin2() {

	}

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

		Iterable<Integer> getFlightDurations();

		double getNumDeparting(int duration, int timePeriod);

		double getEnroute(int i);

	}

	public static double getAverageDiversions(Input input, GRBModel model) throws GRBException {
		double sum = 0.0;
		for (int i = 0; i < input.getNumTimePeriods(); i++) {
			for (int s : input.getScenarios()) {
				sum += model.getVarByName(getDivertVarName(s, i)).get(GRB.DoubleAttr.X) * input.getScenProbability(s);
			}
		}
		return sum;
	}

	public static GRBModel solveModel(Input myInput) throws GRBException {
		return solveModel(myInput, new GRBEnv(), false, -1);
	}

	public static GRBModel solveModel(Input myInput, GRBEnv env, boolean verbose, double maxtime) throws GRBException {
		GRBModel myModel = setupModel(myInput, env, verbose, maxtime);
		if (!verbose) {
			myModel.set(GRB.IntParam.OutputFlag, 0);
		}
		myModel.optimize();
		return myModel;
	}

	public static GRBModel setupModel(Input myInput) throws GRBException {
		return setupModel(myInput, new GRBEnv(), false, -1);
	}

	public static GRBModel setupModel(Input myInput, boolean verbose) throws GRBException {
		return setupModel(myInput, new GRBEnv(), verbose, -1);
	}

	private static GRBModel setupModel(Input myInput, GRBEnv myEnv, boolean verbose, double maxtime)
			throws GRBException {
		GRBModel myModel = new GRBModel(myEnv);
		if (!verbose) {
			myModel.getEnv().set(GRB.IntParam.OutputFlag, 0);
		}
		if (maxtime > 0) {
			myModel.set(GRB.DoubleParam.TimeLimit, maxtime);
		}
		addVars(myModel, myInput);
		myModel.update();
		addConstraints(myModel, myInput);
		myModel.update();
		return myModel;
	}

	private static void addVars(GRBModel myModel, Input myInput) throws GRBException {
		addGroundVars(myModel, myInput);
		addDepartVars(myModel, myInput);
		addAirVars(myModel, myInput);
		addLandVars(myModel, myInput);
		addDivertVars(myModel, myInput);
		addRevisionVars(myModel, myInput);
	}

	private static void addGroundVars(GRBModel model, Input myInput) throws GRBException {
		double groundCost = myInput.getGroundCost();
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			double probability = myInput.getScenProbability(s);
			for (int d : myInput.getFlightDurations()) {
				for (int i = 0; i < numTimePeriods - d; i++) {
					for (int j = i; j < numTimePeriods - d; j++) {
						if(j==i) {
							model.addVar(0.0, GRB.INFINITY, groundCost * probability, GRB.INTEGER,
									getGroundVarName(s, i, j, d));	
						}else {
							model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER,
									getGroundVarName(s, i, j, d));	
						}
						
					}
				}
			}
		}
	}

	private static void addDepartVars(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			for (int d : myInput.getFlightDurations()) {
				for (int i = 0; i < numTimePeriods + 1 - d; i++) {
					for (int j = i; j < numTimePeriods + 1 - d; j++) {
						model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, getDepartVarName(s, i, j, d));
					}
				}
			}
		}

	}

	private static void addAirVars(GRBModel model, Input myInput) throws GRBException {
		double airCost = myInput.getAirCost();
		int numTimePeriods = myInput.getNumTimePeriods();
		double maxAirborne = myInput.getMaxAirborne();

		for (int s : myInput.getScenarios()) {
			double probability = myInput.getScenProbability(s);
			for (int i = 0; i < numTimePeriods; i++) {
				if (maxAirborne != UNLIMITED) {
					model.addVar(0.0, maxAirborne, airCost * probability, GRB.INTEGER, getAirVarName(s, i));
				} else {
					model.addVar(0.0, GRB.INFINITY, airCost * probability, GRB.INTEGER, getAirVarName(s, i));
				}
			}
		}

	}

	private static void addLandVars(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			for (int i = 0; i < numTimePeriods; i++) {
				model.addVar(0.0, myInput.getCapacity(s, i), 0.0, GRB.INTEGER, getLandVarName(s, i));
			}
		}
	}

	private static void addDivertVars(GRBModel model, Input myInput) throws GRBException {
		double divertCost = myInput.getDivertCost();
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			double probability = myInput.getScenProbability(s);
			for (int i = 0; i < numTimePeriods; i++) {
				model.addVar(0.0, GRB.INFINITY, divertCost * probability, GRB.INTEGER, getDivertVarName(s, i));
			}
		}
	}

	private static void addRevisionVars(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			for (int i = 1; i < numTimePeriods; i++) {
				model.addVar(0.0, 1.0, 0.0, GRB.BINARY, getRevisionVarName(s, i));
			}
		}
	}

	public static String getRevisionVarName(int scenario, int timePeriod) {
		return "REVISION: " + scenario + ", " + timePeriod;
	}

	public static String getGroundVarName(int scenario, int timePeriodPlan, int timePeriodAct, int duration) {
		return "GROUND: " + scenario + "," + timePeriodPlan + "," + timePeriodAct + "," + duration;
	}

	public static String getGroundAAConstrName(int scenario1, int scenario2, int timePeriodPlan, int timePeriodAct,
			int duration) {
		return "GROUND_AA: " + scenario1 + "," + scenario2 + "," + timePeriodPlan + "," + timePeriodAct + ","
				+ duration;
	}

	public static String getDepartVarName(int scenario, int timePeriodPlan, int timePeriodAct, int duration) {
		return "DEPART: " + scenario + "," + timePeriodPlan + "," + timePeriodAct + "," + duration;
	}

	public static String getDepartAAConstrName(int scenario1, int scenario2, int timePeriodPlan, int timePeriodAct,
			int duration) {
		return "DEPART_AA: " + scenario1 + "," + scenario2 + "," + timePeriodPlan + "," + timePeriodAct + ","
				+ duration;
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

	public static String getDepartureNodeConstrName(int scenario, int timePeriod, int duration) {
		return "DEP_NODE: " + scenario + "," + timePeriod + "," + duration;
	}

	public static String getArrivalNodeConstrName(int scenario, int timePeriod) {
		return "ARR_NODE: " + scenario + "," + timePeriod;
	}

	private static void addConstraints(GRBModel model, Input myInput) throws GRBException {
		addDepartureNodeConstraints(model, myInput);
		addArrivalNodeConstraints(model, myInput);
		addAntiAnticipatoryConstraints(model, myInput);
		addRevisionConstraints(model, myInput);
		addRevisionLimitConstraints(model, myInput);
	}

	private static void addAntiAnticipatoryConstraints(GRBModel model, Input myInput) throws GRBException {
		// Add anti-anticipatory constraints

		int numTimePeriods = myInput.getNumTimePeriods();
		for (int d : myInput.getFlightDurations()) {
			for (int i = 0; i < numTimePeriods + 1 - d; i++) {
				for (Iterable<Integer> nodes : myInput.getNodes(i)) {
					Iterator<Integer> scenIter = nodes.iterator();
					if (scenIter.hasNext()) {
						int firstScen = scenIter.next();

						while (scenIter.hasNext()) {
							int nextScen = scenIter.next();
							for (int j = i; j < numTimePeriods + 1 - d; j++) {
								GRBVar firstDepartVar = model.getVarByName(getDepartVarName(firstScen, i, j, d));
								GRBVar nextDepartVar = model.getVarByName(getDepartVarName(nextScen, i, j, d));
								model.addConstr(firstDepartVar, GRB.EQUAL, nextDepartVar,
										getDepartAAConstrName(firstScen, nextScen, i, j, d));

								if (j < numTimePeriods - d) {
									GRBVar firstGroundVar = model.getVarByName(getGroundVarName(firstScen, i, j, d));
									GRBVar nextGroundVar = model.getVarByName(getGroundVarName(nextScen, i, j, d));
									model.addConstr(firstGroundVar, GRB.EQUAL, nextGroundVar,
											getGroundAAConstrName(firstScen, nextScen, i, j, d));
								}

							}

						}
					}
				}
			}
		}

		for (int i = 1; i < numTimePeriods; i++) {
			for (Iterable<Integer> nodes : myInput.getNodes(i)) {
				Iterator<Integer> scenIter = nodes.iterator();
				if (scenIter.hasNext()) {
					int firstScen = scenIter.next();
					GRBVar firstRevVar = model.getVarByName(getRevisionVarName(firstScen, i));
					while (scenIter.hasNext()) {
						int nextScen = scenIter.next();
						GRBVar nextRevVar = model.getVarByName(getRevisionVarName(nextScen, i));
						model.addConstr(firstRevVar, GRB.EQUAL, nextRevVar, "");
					}
				}
			}
		}
	}

	private static void addRevisionLimitConstraints(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();

		for (int s : myInput.getScenarios()) {
			GRBLinExpr myExpr = new GRBLinExpr();
			for (int i = 1; i < numTimePeriods; i++) {
				myExpr.addTerm(1.0, model.getVarByName(getRevisionVarName(s, i)));
			}
			model.addConstr(myExpr, GRB.LESS_EQUAL, 1, "");
		}
	}

	private static void addRevisionConstraints(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();

		for (int s : myInput.getScenarios()) {
			for (int d : myInput.getFlightDurations()) {
				for (int i = 1; i < numTimePeriods - d; i++) {
					for (int j = i; j < numTimePeriods - d; j++) {
						GRBLinExpr myExpr = new GRBLinExpr();
						myExpr.addTerm(1.0, model.getVarByName(getGroundVarName(s, i, j, d)));
						myExpr.addTerm(-1.0, model.getVarByName(getGroundVarName(s, i - 1, j, d)));
						model.addGenConstrIndicator(model.getVarByName(getRevisionVarName(s, i)), 0, myExpr, GRB.EQUAL,
								0, "");
					}
				}
			}
		}

		for (int s : myInput.getScenarios()) {
			for (int d : myInput.getFlightDurations()) {
				for (int i = 1; i < numTimePeriods + 1 - d; i++) {
					for (int j = i; j < numTimePeriods +1 - d; j++) {
						GRBLinExpr myExpr = new GRBLinExpr();
						myExpr.addTerm(1.0, model.getVarByName(getDepartVarName(s, i, j, d)));
						myExpr.addTerm(-1.0, model.getVarByName(getDepartVarName(s, i - 1, j, d)));
						model.addGenConstrIndicator(model.getVarByName(getRevisionVarName(s, i)), 0, myExpr, GRB.EQUAL,
								0, "");
					}
				}
			}
		}

	}

	private static void addArrivalNodeConstraints(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int j : myInput.getScenarios()) {

			for (int i = 0; i < numTimePeriods; i++) {
				GRBLinExpr inFlow = new GRBLinExpr();
				inFlow.addConstant(myInput.getEnroute(i));
				for (int k : myInput.getFlightDurations()) {
					if (i - k >= 0) {
						inFlow.addTerm(1.0, model.getVarByName(getDepartVarName(j, i - k, i - k, k)));
					}
				}
				if (i > 0) {
					inFlow.addTerm(1.0, model.getVarByName(getAirVarName(j, i - 1)));
				}
				GRBLinExpr outFlow = new GRBLinExpr();
				outFlow.addTerm(1.0, model.getVarByName(getAirVarName(j, i)));
				outFlow.addTerm(1.0, model.getVarByName(getLandVarName(j, i)));
				outFlow.addTerm(1.0, model.getVarByName(getDivertVarName(j, i)));
				model.addConstr(inFlow, GRB.EQUAL, outFlow, getArrivalNodeConstrName(j, i));
			}
		}
	}

	private static void addDepartureNodeConstraints(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int d : myInput.getFlightDurations()) {
			for (int i = 0; i < numTimePeriods + 1 - d; i++) {
				for (int j : myInput.getScenarios()) {
					GRBLinExpr inFlow = new GRBLinExpr();
					GRBLinExpr outFlow = new GRBLinExpr();
					inFlow.addConstant(myInput.getNumDeparting(d, i));
					if (i > 0) {
						inFlow.addTerm(1.0, model.getVarByName(getGroundVarName(j, i - 1, i - 1, d)));
					}
					if (i < numTimePeriods - d) {
						outFlow.addTerm(1.0, model.getVarByName(getGroundVarName(j, i, i, d)));
					}
					outFlow.addTerm(1.0, model.getVarByName(getDepartVarName(j, i, i, d)));
					model.addConstr(inFlow, GRB.EQUAL, outFlow, getDepartureNodeConstrName(j, i, d));
				}
			}
		}
	}

}
