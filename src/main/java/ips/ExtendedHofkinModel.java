package main.java.ips;

import gurobi.GRB;

import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.Iterator;

public final class ExtendedHofkinModel {
	public final static int UNLIMITED = -1;
	private ExtendedHofkinModel(){
		
	}
	
	public interface Input{
		int getNumTimePeriods();
		
		double getGroundCost();
		double getAirCost();
		int getMaxAirborne();
		
		double getScenProbability(int s);
		Iterable<Integer> getScenarios();
		Iterable<? extends Iterable<Integer>> getNodes(int i);
		double getCapacity(int scenario, int timePeriod);
		
		Iterable<Integer> getFlightDurations();
		double getNumDeparting(int duration, int timePeriod);
		double getEnroute(int i);
		
	}

	public static GRBModel solveModel(Input myInput) throws GRBException{
		return solveModel(myInput,new GRBEnv(),false);
	}
	
	public static GRBModel solveModel(Input myInput,GRBEnv env,boolean verbose) throws GRBException{
		GRBModel myModel = setupModel(myInput,env,verbose);
		if(!verbose) {
			myModel.set(GRB.IntParam.OutputFlag,0);
		}
		myModel.optimize();
		return myModel;
	}
	
	public static GRBModel setupModel(Input myInput) throws GRBException{
		return setupModel(myInput, new GRBEnv(),false);
	}
	
	public static GRBModel setupModel(Input myInput,boolean verbose) throws GRBException{
		return setupModel(myInput, new GRBEnv(),verbose);
	}
	
	private static GRBModel setupModel(Input myInput, GRBEnv myEnv,boolean verbose) throws GRBException{
		GRBModel myModel = new GRBModel(myEnv);
		if(!verbose){
			myModel.getEnv().set(GRB.IntParam.OutputFlag, 0);
		}
		addVars(myModel, myInput);
		myModel.update();
		addConstraints(myModel, myInput);
		myModel.update();
		return myModel;
	}
	
	private static void addVars(GRBModel myModel, Input myInput) throws GRBException {
		addGroundVars(myModel,myInput);
		addDepartVars(myModel,myInput);
		addAirVars(myModel,myInput);
	}

	private static void addGroundVars(GRBModel model, Input myInput)
			throws GRBException {
		double groundCost = myInput.getGroundCost();
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			double probability = myInput.getScenProbability(s);
			for (int i = 0; i < numTimePeriods-1; i++) {
				for (int d : myInput.getFlightDurations()) {
					model.addVar(0.0, GRB.INFINITY, groundCost * probability,
							GRB.INTEGER, getGroundVarName(s, i, d));
				}
			}
		}

	}

	private static void addDepartVars(GRBModel model, Input myInput)
			throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int s : myInput.getScenarios()) {
			for (int i = 0; i < numTimePeriods; i++) {
				for (int d : myInput.getFlightDurations()) {
					model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER,
							getDepartVarName(s, i, d));
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

	public static String getGroundVarName(int scenario, int timePeriod, int duration){
		return "GROUND: " + scenario + "," + timePeriod + "," + duration;
	}
	
	public static String getGroundAAConstrName(int scenario1, int scenario2, int timePeriod, int duration){
		return "GROUND_AA: " + scenario1 + ","+ scenario2 + "," + timePeriod + "," + duration;
	}
	
	public static String getDepartVarName(int scenario, int timePeriod, int duration){
		return "DEPART: " + scenario + "," + timePeriod + "," + duration;
	}
	
	public static String getDepartAAConstrName(int scenario1, int scenario2, int timePeriod, int duration){
		return "DEPART_AA: " + scenario1 + ","+ scenario2 + "," + timePeriod + "," + duration;
	}
	
	public static String getAirVarName(int scenario, int timePeriod){
		return "AIR: " + scenario + "," + timePeriod;
	}
	
	public static String getDepartureNodeConstrName(int scenario, int timePeriod, int duration){
		return "DEP_NODE: " + scenario + "," + timePeriod + "," + duration;
	}
	
	public static String getArrivalNodeConstrName(int scenario, int timePeriod){
		return "ARR_NODE: " + scenario + "," + timePeriod;
	}
	private static void addConstraints(GRBModel model, Input myInput) throws GRBException{
		addDepartureNodeConstraints(model,myInput);
		addArrivalNodeConstraints(model,myInput);
		addAntiAnticipatoryConstraints(model,myInput);
	}
	
	private static void addAntiAnticipatoryConstraints(GRBModel model,
			Input myInput) throws GRBException {
		//Add anti-anticipatory constraints

		int numTimePeriods = myInput.getNumTimePeriods();
		for(int i=0; i < numTimePeriods; i++){
			for(Iterable<Integer> nodes: myInput.getNodes(i)){
				Iterator<Integer> scenIter = nodes.iterator();
				if(scenIter.hasNext()){
					int firstScen = scenIter.next();
					
					while(scenIter.hasNext()){
						int nextScen = scenIter.next();
						for (int d : myInput.getFlightDurations()) {
							GRBVar firstDepartVar = model.getVarByName(getDepartVarName(firstScen, i, d));
							GRBVar nextDepartVar = model.getVarByName(getDepartVarName(nextScen, i, d));
							model.addConstr(firstDepartVar, GRB.EQUAL, nextDepartVar, getDepartAAConstrName(firstScen, nextScen, i, d));
							
							if(i < numTimePeriods-1) {
								GRBVar firstGroundVar = model.getVarByName(getGroundVarName(firstScen, i, d));
								GRBVar nextGroundVar = model.getVarByName(getGroundVarName(nextScen, i, d));
								model.addConstr(firstGroundVar, GRB.EQUAL,  nextGroundVar, getDepartAAConstrName(firstScen, nextScen, i, d));
							}
						}
					}
				}
			}
		}
			
	}

	private static void addArrivalNodeConstraints(GRBModel model, Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for(int j : myInput.getScenarios()){

			for(int i =0;i < numTimePeriods;i++){
				GRBLinExpr inFlow = new GRBLinExpr();
				inFlow.addConstant(myInput.getEnroute(i));
				for(int k: myInput.getFlightDurations()){
					if(i - k >= 0){
						inFlow.addTerm(1.0, model.getVarByName(getDepartVarName(j, i-k, k)));
					}
				}
				if(i > 0){
					inFlow.addTerm(1.0, model.getVarByName(getAirVarName(j, i-1)));
				}
				GRBLinExpr outFlow = new GRBLinExpr();
				outFlow.addConstant(myInput.getCapacity(j,i));
				outFlow.addTerm(1.0, model.getVarByName(getAirVarName(j, i)));
				model.addConstr(inFlow, GRB.LESS_EQUAL,outFlow,getArrivalNodeConstrName(j, i));
			}
		}
	}

	private static void addDepartureNodeConstraints(GRBModel model,
			Input myInput) throws GRBException {
		int numTimePeriods = myInput.getNumTimePeriods();
		for (int d : myInput.getFlightDurations()) {
			for (int i = 0; i < numTimePeriods; i++) {
				for (int j : myInput.getScenarios()) {
					GRBLinExpr inFlow = new GRBLinExpr();
					GRBLinExpr outFlow = new GRBLinExpr();
					inFlow.addConstant(myInput.getNumDeparting(d, i));
					if (i > 0) {
						inFlow.addTerm(1.0, model
								.getVarByName(getGroundVarName(j, i - 1, d)));
					}
					if(i < numTimePeriods -1){
						outFlow.addTerm(1.0,
								model.getVarByName(getGroundVarName(j, i, d)));
					}
					outFlow.addTerm(1.0,
							model.getVarByName(getDepartVarName(j, i, d)));
					model.addConstr(inFlow, GRB.EQUAL, outFlow,
							getDepartureNodeConstrName(j, i, d));
				}
			}
		}
	}
	
}
