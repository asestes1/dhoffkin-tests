package util;

import java.time.Duration;
import java.time.OffsetDateTime;

public final class DiscretizerUtil {
	private DiscretizerUtil(){
		
	}
	
	public static int getNumTimePeriods(OffsetDateTime start, OffsetDateTime end, Duration discretization){
		int indexOfLast = timeToIndex(end, start, discretization);
		if(end.equals(start.plus(discretization.multipliedBy(indexOfLast)))){
			return indexOfLast;
		}
		return indexOfLast+1;
				
	}
	
	public static int timeToIndex(OffsetDateTime time, OffsetDateTime start, Duration discretization){
		return durationToTimePeriods(Duration.between(start,time), discretization);
	}
	public static int durationToTimePeriods(Duration dur, Duration discretization){
		return (int) ((double) dur.toNanos()/discretization.toNanos());
	}
}
