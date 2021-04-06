package ips;

import gurobi.GRB;

import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.Iterator;

public final class LRHoffkin {
	public final static int UNLIMITED = -1;

	private LRHoffkin() {

	}

	public interface Input {
		int getNumTimePeriods();

		double getGroundCost();

		double getAirCost();

		double getDivertCost();

		int getMaxAirborne();

		double getScenProbability(int s);

		Iterable<? extends Integer> getScenarios();

		Iterable<? extends Iterable<? extends Integer>> getNodes(int i);

		double getCapacity(int scenario, int timePeriod);

		Iterable<? extends Integer> getFlightDurations();

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
		return solveModel(myInput, new GRBEnv(), false);
	}

	public static GRBModel solveModel(Input myInput, GRBEnv env, boolean verbose) throws GRBException {
		GRBModel myModel = setupModel(myInput, env, verbose);
		if (!verbose) {
			myModel.set(GRB.IntParam.OutputFlag, 0);
		}
		myModel.optimize();
		return myModel;
	}

	public static GRBModel setupModel(Input myInput) throws GRBException {
		return setupModel(myInput, new GRBEnv(), false);
	}

	public static GRBModel setupModel(Input myInput, boolean verbose) throws GRBException {
		return setupModel(myInput, new GRBEnv(), verbose);
	}

	private static GRBModel setupModel(Input myInput, GRBEnv myEnv, boolean verbose) throws GRBException {
		GRBModel myModel = new GRBModel(myEnv);
		if (!verbose) {
			myModel.getEnv().set(GRB.IntParam.OutputFlag, 0);
		}
		addVars(myModel, myInput);
		myModel.update();
		addConstraints(myModel, myInput);
		myModel.update();
		return myModel;
	}

	private static void addVars(GRBModel myModel, Input myInput) throws GRBException {
		addGroundVars(myModel, myInput);
		addGroundRevisionVars(myModel, myInput);
		addDepartVars(myModel, myInput);
		addDepartRevisionVars(myModel, myInput);
		addRevisionVars(myModel, myInput);
		addAirVars(myModel, myInput);
		addLandVars(myModel, myInput);
		addDivertVars(myModel, myInput);
	}

	private static void addRevisionVars(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for(int s: myInput.getScenarios()) {
			for (int i = 0; i < numTimePeriods; i++) {
				model.addVar(0.0, 1.0, 0.0, GRB.BINARY, getRevisionVarName(s,i));
			}
		}
	}

	private static void addGroundVars(GRBModel model, Input myInput) throws GRBException {
		double groundCost = myInput.getGroundCost();
		int numTimePeriods = myInput.getNumTimePeriods();

		for (int d : myInput.getFlightDurations()) {
			for (int i = 0; i < numTimePeriods - d; i++) {
				model.addVar(0.0, GRB.INFINITY, groundCost, GRB.INTEGER, getGroundVarName(i, d));
			}
		}
	}

	private static void addGroundRevisionVars(GRBModel model, Input myInput) throws GRBException {
		double groundCost = myInput.getGroundCost();
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			for (int d : myInput.getFlightDurations()) {
				for (int i = 0; i < numTimePeriods - d; i++) {
					for (int j = i; j < numTimePeriods - d; j++) {
						model.addVar(-GRB.INFINITY, GRB.INFINITY, groundCost * myInput.getScenProbability(s),
								GRB.INTEGER, getGroundRevisionVarName(s, i, j, d));
					}
				}
			}
		}

	}

	private static void addDepartVars(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int d : myInput.getFlightDurations()) {
			for (int i = 0; i < numTimePeriods + 1 - d; i++) {
				model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, getDepartVarName(i, d));
			}
		}
	}

	private static void addDepartRevisionVars(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			for (int d : myInput.getFlightDurations()) {
				for (int i = 0; i < numTimePeriods; i++) {
					for (int j = i; j < numTimePeriods; j++) {
						model.addVar(-GRB.INFINITY, GRB.INFINITY, 0.0, GRB.INTEGER,
								getDepartRevisionVarName(s, i, j, d));
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

	public static String getRevisionVarName(int scenario, int timePeriod) {
		return "REVISION: " + scenario +", "+timePeriod;
	}

	public static String getGroundVarName(int timePeriod, int duration) {
		return "GROUND: " + timePeriod + ", " + duration;
	}

	public static String getGroundRevisionVarName(int scenario, int revisionTime, int delayTime, int duration) {
		return "GROUND_REV: " + scenario + ", " + revisionTime + ", " + delayTime + ", " + duration;
	}

	public static String getGroundAAConstrName(int scenario1, int scenario2, int timePeriod, int duration) {
		return "GROUND_AA: " + scenario1 + ", " + scenario2 + ", " + timePeriod + ", " + duration;
	}

	public static String getDepartVarName(int timePeriod, int duration) {
		return "DEPART: " + timePeriod + ", " + duration;
	}

	public static String getDepartRevisionVarName(int scenario, int revisionTime, int depTime, int duration) {
		return "DEPART_REV: " + scenario + ", " + revisionTime + ", " + depTime + ", " + duration;
	}

	public static String getDepartAAConstrName(int scenario1, int scenario2, int timePeriod, int duration) {
		return "DEPART_AA: " + scenario1 + ", " + scenario2 + ", " + timePeriod + ", " + duration;
	}

	public static String getAirVarName(int scenario, int timePeriod) {
		return "AIR: " + scenario + ", " + timePeriod;
	}

	public static String getLandVarName(int scenario, int timePeriod) {
		return "LAND: " + scenario + ", " + timePeriod;
	}

	public static String getDivertVarName(int scenario, int timePeriod) {
		return "DIVERT: " + scenario + ", " + timePeriod;
	}

	public static String getDepartureNodeConstrName(int scenario, int timePeriod, int duration) {
		return "DEP_NODE: " + scenario + ", " + timePeriod + ", " + duration;
	}

	public static String getArrivalNodeConstrName(int scenario, int timePeriod) {
		return "ARR_NODE: " + scenario + ", " + timePeriod;
	}

	private static void addConstraints(GRBModel model, Input myInput) throws GRBException {
		addDepartureNodeConstraints(model, myInput);
		addArrivalNodeConstraints(model, myInput);
		addAntiAnticipatoryConstraints(model, myInput);
		addRevisionNonnegativityConstr(model, myInput);
		addRevisionConstr(model, myInput);
	}

	private static void addRevisionNonnegativityConstr(GRBModel model, Input myInput) throws GRBException {

		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			for (int d : myInput.getFlightDurations()) {
				for (int i = 0; i < numTimePeriods - d; i++) {
					GRBLinExpr myexpr = new GRBLinExpr();
					myexpr.addTerm(1.0, model.getVarByName(getGroundVarName(i, d)));

					for (int t = 0; t <= i; t++) {
						myexpr.addTerm(1.0, model.getVarByName(getGroundRevisionVarName(s, t, i, d)));
					}
					model.addConstr(myexpr, GRB.GREATER_EQUAL, 0.0, "");

				}
			}
		}

		for (int s : myInput.getScenarios()) {
			for (int d : myInput.getFlightDurations()) {
				for (int i = 0; i < numTimePeriods + 1 -d; i++) {
					GRBLinExpr myexpr = new GRBLinExpr();
					myexpr.addTerm(1.0, model.getVarByName(getDepartVarName(i, d)));

					for (int t = 0; t <= i; t++) {
						myexpr.addTerm(1.0, model.getVarByName(getDepartRevisionVarName(s, t, i, d)));
					}
					model.addConstr(myexpr, GRB.GREATER_EQUAL, 0.0, "");
				}
			}
		}

	}

	private static void addRevisionConstr(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();

		for (int s : myInput.getScenarios()) {
			GRBLinExpr myexpr = new GRBLinExpr();

			for (int i = 0; i < numTimePeriods; i++) {
				myexpr.addTerm(1.0, model.getVarByName(getRevisionVarName(s,i)));
				model.addConstr(myexpr, GRB.LESS_EQUAL, 1.0, "");
				
			}
		}
		
		for (int s : myInput.getScenarios()) {
			for(int d: myInput.getFlightDurations()) {
				for(int revTime = 0; revTime < numTimePeriods-d; revTime++) {
					for(int time=revTime; time < numTimePeriods-d; time++) {
						GRBVar delayvar = model.getVarByName(getGroundRevisionVarName(s, revTime, time, d));
						GRBVar revvar = model.getVarByName(getRevisionVarName(s,revTime));
						GRBLinExpr myexpr = new GRBLinExpr();
						myexpr.addTerm(1.0, delayvar);
						model.addGenConstrIndicator(revvar, 0, myexpr, GRB.EQUAL, 0.0, "");
					}
				}
			}
		}
		
		for (int s : myInput.getScenarios()) {
			for(int d: myInput.getFlightDurations()) {
				for(int revTime = 0; revTime < numTimePeriods+1-d; revTime++) {
					for(int time=revTime; time < numTimePeriods+1-d; time++) {
						GRBVar departvar = model.getVarByName(getDepartRevisionVarName(s, revTime, time, d));
						GRBVar revvar = model.getVarByName(getRevisionVarName(s,revTime));
						GRBLinExpr myexpr = new GRBLinExpr();
						myexpr.addTerm(1.0, departvar);
						model.addGenConstrIndicator(revvar, 0, myexpr, GRB.EQUAL, 0.0, "");
					}
				}
			}
		}
	}

	private static void addAntiAnticipatoryConstraints(GRBModel model, Input myInput) throws GRBException {
		// Add anti-anticipatory constraints

		int numTimePeriods = myInput.getNumTimePeriods();
		for (int d : myInput.getFlightDurations()) {
			for (int i = 0; i < numTimePeriods + 1 - d; i++) {
				for (int t = 0; t <= i; t++) {
					for (Iterable<? extends Integer> nodes : myInput.getNodes(t)) {
						Iterator<? extends Integer> scenIter = nodes.iterator();
						if (scenIter.hasNext()) {
							int firstScen = scenIter.next();

							while (scenIter.hasNext()) {
								int nextScen = scenIter.next();
								GRBVar firstDepartVar = model
										.getVarByName(getDepartRevisionVarName(firstScen, t, i, d));
								GRBVar nextDepartVar = model.getVarByName(getDepartRevisionVarName(nextScen, t, i, d));
								model.addConstr(firstDepartVar, GRB.EQUAL, nextDepartVar,
										getDepartAAConstrName(firstScen, nextScen, i, d));

								if (i < numTimePeriods - d) {
									GRBVar firstGroundVar = model
											.getVarByName(getGroundRevisionVarName(firstScen, t, i, d));
									GRBVar nextGroundVar = model
											.getVarByName(getGroundRevisionVarName(nextScen, t, i, d));
									model.addConstr(firstGroundVar, GRB.EQUAL, nextGroundVar,
											getGroundAAConstrName(firstScen, nextScen, i, d));
								}
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < numTimePeriods; i++) {
			for (Iterable<? extends Integer> nodes : myInput.getNodes(i)) {
				Iterator<? extends Integer> scenIter = nodes.iterator();
				if (scenIter.hasNext()) {
					int firstScen = scenIter.next();

					while (scenIter.hasNext()) {
						int nextScen = scenIter.next();
						GRBVar firstDivertVar = model.getVarByName(getDivertVarName(firstScen, i));
						GRBVar nextDivertVar = model.getVarByName(getDivertVarName(nextScen, i));
						model.addConstr(firstDivertVar, GRB.EQUAL, nextDivertVar, "");

						GRBVar firstAirVar = model.getVarByName(getAirVarName(firstScen, i));
						GRBVar nextAirVar = model.getVarByName(getAirVarName(nextScen, i));
						model.addConstr(firstAirVar, GRB.EQUAL, nextAirVar, "");

						GRBVar firstLandVar = model.getVarByName(getLandVarName(firstScen, i));
						GRBVar nextLandVar = model.getVarByName(getLandVarName(nextScen, i));
						model.addConstr(firstLandVar, GRB.EQUAL, nextLandVar, "");
						
						GRBVar firstRevisionVar = model.getVarByName(getRevisionVarName(firstScen, i));
						GRBVar nextRevisionVar = model.getVarByName(getRevisionVarName(nextScen, i));
						model.addConstr(firstRevisionVar, GRB.EQUAL, nextRevisionVar, "");

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
						inFlow.addTerm(1.0, model.getVarByName(getDepartVarName(i - k, k)));
						for (int t = 0; t <= i-k; t++) {
							inFlow.addTerm(1.0, model.getVarByName(getDepartRevisionVarName(j, t, i - k, k)));
						}
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
						inFlow.addTerm(1.0, model.getVarByName(getGroundVarName(i - 1, d)));
					}
					for (int t = 0; t <= i - 1; t++) {
						inFlow.addTerm(1.0, model.getVarByName(getGroundRevisionVarName(j, t, i - 1, d)));
					}

					if (i < numTimePeriods - d) {
						outFlow.addTerm(1.0, model.getVarByName(getGroundVarName(i, d)));
						for (int t = 0; t <= i; t++) {
							outFlow.addTerm(1.0, model.getVarByName(getGroundRevisionVarName(j, t, i, d)));
						}
					}
					
					outFlow.addTerm(1.0, model.getVarByName(getDepartVarName(i, d)));
					for (int t = 0; t <= i; t++) {
						outFlow.addTerm(1.0, model.getVarByName(getDepartRevisionVarName(j, t, i, d)));
					}
					model.addConstr(inFlow, GRB.EQUAL, outFlow, getDepartureNodeConstrName(j, i, d));
				}
			}
		}
	}

}
