package main.java.scenarios;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DiscreteScenarioTree {
	private final HashMap<Integer,DiscreteScenario> scenarios;
	private final Map<Integer,HashSet<HashSet<Integer>>> tree;
	
	public DiscreteScenarioTree(int numTimePeriods, Collection<DiscreteScenario> scenarios, int lookahead){
		this.scenarios = new HashMap<Integer,DiscreteScenario>();
		for(DiscreteScenario s: scenarios){
			this.scenarios.put(s.getId(), s);
		}
		tree = makeTree(this.scenarios, numTimePeriods, lookahead);
	}

	private static Map<Integer, HashSet<HashSet<Integer>>> makeTree(
			HashMap<Integer,DiscreteScenario> scenarios, int numTimePeriods, int lookahead) {
		HashMap<Integer,HashSet<HashSet<Integer>>> myNodeTree = new HashMap<Integer,HashSet<HashSet<Integer>>>();
		for (int i = 0; i < numTimePeriods; i++) {
			HashSet<HashSet<Integer>> myNodes = new HashSet<HashSet<Integer>>();
			for (DiscreteScenario s : scenarios.values()) {
				addToPartition(i,s,myNodes,scenarios,lookahead);
			}
			myNodeTree.put(i, myNodes);
		}
		return myNodeTree;
	}
	
	private static void addToPartition(int timePeriod, DiscreteScenario s,
			HashSet<HashSet<Integer>> partition,
			HashMap<Integer, DiscreteScenario> scenarios, int lookahead) {
		Iterator<HashSet<Integer>> myNodeIter = partition.iterator();
		while (myNodeIter.hasNext()) {
			HashSet<Integer> node = myNodeIter.next();
			DiscreteScenario nodeScenario = scenarios.get(node.iterator()
					.next());
			// If we find an existing node that the scenario belongs to, then
			// add the scenario to that node.
			if (nodeScenario.equals(s, timePeriod, lookahead)) {
				myNodeIter.remove();
				node.add(s.getId());
				partition.add(node);
				return;
			}

		}
		// If no existing node was found, then make a new node.
		HashSet<Integer> newNode = new HashSet<Integer>();
		newNode.add(s.getId());
		partition.add(newNode);
	}
	
	public Set<Integer> getScenarioIds(){
		return new HashSet<Integer>(scenarios.keySet());
	}
	
	public HashSet<HashSet<Integer>> getScenarioNodes(int timePeriod){
		HashSet<HashSet<Integer>> copy = new HashSet<HashSet<Integer>>();
		for(HashSet<Integer> node: tree.get(timePeriod)){
			copy.add(new HashSet<Integer>(node));
		}
		return copy;
	}
	
	@Override
	public String toString(){
		ArrayList<Integer> indices = new ArrayList<Integer>(tree.keySet());
		Collections.sort(indices);
		String myString = "";
		for(int i: indices){
			myString+= i+": {";
			for(HashSet<Integer> node: tree.get(i)){
				myString+= "{";
				for(int s: node){
					myString+= s +", ";
				}
				myString = myString.substring(0, myString.length()-2);
				myString+= "}, ";
			}
			myString = myString.substring(0, myString.length()-2);
			myString+="}\n";
		}
		return myString;
	}
	
	public String allScenariosToString(){
		String allScenarios = "";
		for(DiscreteScenario s: scenarios.values()){
			allScenarios += s.toString()+"\n";
		}
		return allScenarios;
	}
	
	public DiscreteScenario getScenario(int scenarioId){
		return scenarios.get(scenarioId);
	}
}
