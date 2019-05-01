package main.java.scenarios;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a discretized version of a capacity scenario. 
 * Instead of containing times at which the capacity changes,
 * a discrete capacity scenario describes the number of flights
 * which may arrive in each discrete time period.
 * @author Alex2
 *
 */
public class DiscreteScenario {
	private final int id;
	private final double probability;
	private final List<Integer> capacity;
	
	/**
	 * Standard constructor. Deep-copy of capacity list is made 
	 * in order to make this class immutable.
	 * @param probability
	 * @param capacity
	 */
	public DiscreteScenario(int id, double probability, List<Integer> capacity){
		this.id = id;
		this.probability = probability;
		this.capacity = new ArrayList<Integer>(capacity);
	}

	public double getProbability() {
		return probability;
	}

	public List<Integer> getCapacities() {
		return new ArrayList<Integer>(capacity);
	}
	
	public int getCapacity(int index){
		return capacity.get(index);
	}
	
	public int getId() {
		return id;
	}

	public int getNumTimePeriods(){
		return capacity.size();
	}
	
	public boolean equals(DiscreteScenario other, int index, int lookahead){
		int lastIndex = index+lookahead;
		if(lastIndex >= getNumTimePeriods()){
			lastIndex = getNumTimePeriods()-1;
		}
		for(int i=0; i <= lastIndex;i++){
			if(other.getCapacity(i) != this.getCapacity(i)){
				return false;
			}
		}
		return true;
	}
	@Override
	public String toString(){
		String myString = "P="+probability+", C:";
		Iterator<Integer> myIter = capacity.iterator();
		while(myIter.hasNext()){
			myString += myIter.next();
			if(myIter.hasNext()){
				myString+=",";
			}
		}
		return myString;
	}
	
	
}
