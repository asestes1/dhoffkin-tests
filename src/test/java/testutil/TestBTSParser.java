package testutil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import util.BTSParser;
import util.BTSParser.ResultStruct;

public class TestBTSParser {

	@Test
	public void testParseBTS() throws IOException{
		System.out.println(parseSimpleDataset());
	}
	
	public static ResultStruct parseSimpleDataset() throws IOException{
		File btsFile = new File(TestBTSParser.class.getClassLoader().getResource("OnTime_2017_07_15.csv").getFile());
		System.out.println(btsFile.toString());
		OffsetDateTime startTime = LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(ZoneOffset.UTC);
		OffsetDateTime endTime = LocalDateTime.of(2017, 7, 15, 15, 0).atOffset(ZoneOffset.UTC);
		return BTSParser.separateForGDPPlanning(BTSParser.filterByAirportAndTimeRange(btsFile, startTime , endTime,  "EWR"),startTime,endTime);
	}
}
