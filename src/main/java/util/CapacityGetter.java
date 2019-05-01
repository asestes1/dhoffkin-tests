package main.java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public final class CapacityGetter {
	private static HashMap<String,Pair<Integer,Integer>> airportToVfrIfr;
	private static final boolean initialized;
	private static final IOException initException;
	
	static{
		InputStream capacityFile = findCapacityFile(); 
		boolean initSuccess = true;
		IOException caughtException = null;
		try {
			airportToVfrIfr = parseCapacityFile(capacityFile);
		} catch (IOException e) {
			initSuccess = false;
			caughtException = e;
		}
		initialized=initSuccess;
		initException=caughtException;
	}
	
	private static HashMap<String, Pair<Integer,Integer>> parseCapacityFile(InputStream capacityfile) throws IOException{
		HashMap<String,Pair<Integer,Integer>> map = new HashMap<String,Pair<Integer,Integer>>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(capacityfile));
		CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withHeader());
		
		
		for(CSVRecord record: parser){
			String airport = record.get("AIRPORT").trim();
			int vfr = Integer.parseInt(record.get("VFR"));
			int ifr = Integer.parseInt(record.get("IFR"));
			map.put(airport,ImmutablePair.of(vfr, ifr));
		}
		parser.close();
		reader.close();
		capacityfile.close();
		return map;
	}
	
	private static InputStream findCapacityFile() {
		ClassLoader thisLoader = CapacityGetter.class.getClassLoader();
		return thisLoader.getResourceAsStream("util/est_capacities.csv");
	}

	public static Integer getVfrOrIfr(String airport, boolean isVfr) throws IOException{
		if(initialized){
			String key = airport.toUpperCase().trim();
			if(airportToVfrIfr.containsKey(key)){
				if(isVfr){
					return airportToVfrIfr.get(key).getLeft();
				}
				return airportToVfrIfr.get(key).getRight();
			}else{
				throw new IllegalArgumentException("Airport not found in vfr/ifr database: "+key);
			}
		}
		throw initException;
	}
	public static Integer getIfr(String airport) throws IOException{
		return getVfrOrIfr(airport, false);
	}
	
	public static Integer getVfr(String airport) throws IOException{
		return getVfrOrIfr(airport, true);
	}
	
	private CapacityGetter(){
		
	}
	
}
