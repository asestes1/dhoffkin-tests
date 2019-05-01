package main.java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.util.HashMap;

public final class TimeZoneGetter {
	private TimeZoneGetter(){
		
	}
	
	private static HashMap<String,ZoneId> airportToTimeZone;
	private static final boolean initialized;
	private static final IOException initException;
	
	static{
		InputStream timeZoneFile = findTimeZoneFile(); 
		boolean initSuccess = true;
		IOException caughtException = null;
		try {
			airportToTimeZone = parseTimeZoneFile(timeZoneFile);
		} catch (IOException e) {
			initSuccess = false;
			caughtException = e;
		}
		initialized=initSuccess;
		initException=caughtException;
	}
	
	private static HashMap<String, ZoneId> parseTimeZoneFile(InputStream timezonefile) throws IOException{
		HashMap<String,ZoneId> map = new HashMap<String,ZoneId>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(timezonefile));
		String line = "START";
		while((line = reader.readLine()) != null){
			String[] elements = line.split("\\s+");
			String airport = elements[0];
			ZoneId tz = ZoneId.of(elements[1]);
			map.put(airport, tz);
		}
		reader.close();
		timezonefile.close();
		return map;
	}
	
	private static InputStream findTimeZoneFile() {
		ClassLoader thisLoader = TimeZoneGetter.class.getClassLoader();
		return thisLoader.getResourceAsStream("util/IATA TZ Map.txt");
	}

	public static ZoneId getTimeZone(String airport) throws IOException{
		if(initialized){
			String key = airport.toUpperCase().trim();
			if(airportToTimeZone.containsKey(key)){
				return airportToTimeZone.get(key);
			}else{
				throw new IllegalArgumentException("Airport not found in time zone database: "+key);
			}
		}
		throw initException;
	}
}
