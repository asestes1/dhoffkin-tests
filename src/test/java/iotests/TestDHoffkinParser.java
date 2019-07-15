package iotests;

import ips.DHoffkinFlightParser;

import java.io.IOException;
import java.time.Duration;

import org.junit.Test;

import util.BTSParser;
import testutil.TestBTSParser;

public class TestDHoffkinParser {
	
	public static DHoffkinFlightParser.DemandStruct wrapSimpleDataset() throws IOException{
		Duration discretization = Duration.ofMinutes(10);
		BTSParser.ResultStruct results = TestBTSParser.parseSimpleDataset();
		return DHoffkinFlightParser.wrapBTSOutput(results,discretization);
	}
	
	@Test
	public void testParseBTS() throws IOException{
		System.out.println(wrapSimpleDataset());
	}
}
