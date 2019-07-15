package ips;

public class DiscreteFlight {
	private final int flightId;
	private final int departTimePeriod;
	private final int flightDuration;
	
	public DiscreteFlight(int flightId, int departTimePeriod, int flightDuration) {
		super();
		this.flightId = flightId;
		this.departTimePeriod = departTimePeriod;
		this.flightDuration = flightDuration;
	}

	public int getDepartTimePeriod() {
		return departTimePeriod;
	}


	public int getFlightDuration() {
		return flightDuration;
	}

	public int getFlightId(){
		return flightId;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof DiscreteFlight){
			return ((DiscreteFlight) o).flightId == flightId;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return flightId;
	}
}
