package test.java.testscenario;

import org.junit.Test;

import main.java.scenarios.DiscreteScenarioTree;
import main.java.scenarios.ScenarioTreeFactory;

public class TestScenarioTreeFactory {

	@Test
	public void testMakeLoToHigh(){
		DiscreteScenarioTree myTree = ScenarioTreeFactory.makeLoToHigh(96, 4, 16, 0,30,4,true,0);
		System.out.println(myTree);
		System.out.println(myTree.allScenariosToString());
		
	}
}
