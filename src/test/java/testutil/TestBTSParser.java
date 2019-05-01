package test.java.testutil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import main.java.util.BTSParser;
import main.java.util.BTSParser.ResultStruct;

public class TestBTSParser {
	public static final File btsDir = new File("/export/scratch/Datasets/BTS Ontime");
	
	@Test
	public void testParseBTS() throws IOException{
		parseSimpleDataset();
		System.out.println(parseSimpleDataset());
	}
	
	public static ResultStruct parseSimpleDataset() throws IOException{
		File btsFile = BTSParser.getBTSFileFromDirectory(btsDir, 2017, 7);
		OffsetDateTime startTime = LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(ZoneOffset.UTC);
		OffsetDateTime endTime = LocalDateTime.of(2017, 7, 15, 15, 0).atOffset(ZoneOffset.UTC);
		return BTSParser.separateForGDPPlanning(BTSParser.filterByAirportAndTimeRange(btsFile, startTime , endTime,  "EWR"),startTime,endTime);
	}
}
