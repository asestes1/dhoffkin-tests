package util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class BTSParser {
    public static final String COL_NAME_YEAR = "Year";
    public static final String COL_NAME_MONTH = "Month";
    public static final String COL_NAME_DAY = "DayofMonth";
    public static final String COL_NAME_DEST_APT = "Dest";

    public static final String COL_NAME_ORIGIN_APT = "Origin";
    public static final String COL_NAME_DEP_TIME = "CRSDepTime";
    public static final String COL_NAME_ARR_TIME = "CRSArrTime";
    public static final String COL_NAME_DURATION = "CRSElapsedTime";

    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("Hmm");
    public static final DateTimeFormatter DEP_TIME_FORMAT =TIME_FORMAT;
    public static final DateTimeFormatter ARR_TIME_FORMAT = TIME_FORMAT;

    public static class ResultStruct {
        private final HashSet<FlightStruct> sittingFlights;
        private final HashSet<FlightStruct> airborneFlights;
        private final OffsetDateTime startTime;
        private final OffsetDateTime endTime;

        public ResultStruct(Collection<FlightStruct> sittingFlights,
                            Collection<FlightStruct> airborneFlights,
                            OffsetDateTime startTime,
                            OffsetDateTime endTime) {
            super();
            this.sittingFlights = new HashSet<FlightStruct>(sittingFlights);
            this.airborneFlights = new HashSet<FlightStruct>(airborneFlights);
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public Set<FlightStruct> getSittingFlights() {
            return sittingFlights;
        }

        public Set<FlightStruct> getAirborneFlights() {
            return airborneFlights;
        }


        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public OffsetDateTime getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            String myString = "Sitting Flights: \n";
            for (FlightStruct f : sittingFlights) {
                myString += "\t" + f.toString() + "\n";
            }

            myString += "Airborne Flights: \n";
            for (FlightStruct f : airborneFlights) {
                myString += "\t" + f.toString() + "\n";
            }
            return myString;

        }
    }

    public static class FlightStruct {
        private final int flightId;
        private final OffsetDateTime depTime;
        private final OffsetDateTime arrTime;
        private final Duration flightDuration;

        public FlightStruct(int flightId, OffsetDateTime depTime,
                            OffsetDateTime arrTime, Duration flightDuration) {
            super();
            this.flightId = flightId;
            this.depTime = depTime;
            this.arrTime = arrTime;
            this.flightDuration = flightDuration;
        }

        public OffsetDateTime getDepartureTime() {
            return depTime;
        }

        public OffsetDateTime getArrivalTime() {
            return arrTime;
        }

        public Duration getFlightDuration() {
            return flightDuration;
        }

        public int getFlightId() {
            return flightId;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof FlightStruct) {
                return ((FlightStruct) o).flightId == flightId;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return flightId;
        }

        @Override
        public String toString() {
            return "FID: " + flightId + ", Dur: " + flightDuration
                    + ", Dep. Time: "
                    + depTime.atZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    + ", Arr. Time: "
                    + arrTime.atZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    public static File getBTSFileFromDirectory(File directory, int year,
                                               int month) {
        return new File(directory, "On_Time_On_Time_Performance_" + year + "_"
                + month + ".csv");
    }

    public static HashSet<FlightStruct> filterByAirportAndTimeRange(File btsFile, OffsetDateTime startTime,
                                                                    OffsetDateTime endTime, String airport) throws IOException {
        Reader in = new FileReader(btsFile);
        CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader());

        HashSet<FlightStruct> flights = new HashSet<FlightStruct>();
        int flightID = 0;
        for (CSVRecord record : parser) {
            if (record.get(COL_NAME_DEST_APT).trim().equalsIgnoreCase(airport)) {
                Duration duration = parseDuration(record);
                OffsetDateTime depTime = parseDepZonedDateTime(record)
                        .toOffsetDateTime();
                OffsetDateTime arrTime = depTime.plus(duration);
                if (!arrTime.isBefore(startTime) && arrTime.isBefore(endTime)) {
                    flights.add(new FlightStruct(flightID, depTime,
                            arrTime, duration));
                    flightID++;
                }
            }

        }
        in.close();
        parser.close();
        return flights;
    }

    public static ResultStruct separateForGDPPlanning(HashSet<FlightStruct> flights,
                                                      OffsetDateTime startTime, OffsetDateTime endTime)
            throws IOException {
        HashSet<FlightStruct> sittingFlights = new HashSet<FlightStruct>();
        HashSet<FlightStruct> airborneFlights = new HashSet<FlightStruct>();
        for (FlightStruct f : flights) {
            if (!f.getArrivalTime().isBefore(startTime)
                    && f.getArrivalTime().isBefore(endTime)) {
                if (f.getDepartureTime().isBefore(startTime)) {
                    airborneFlights.add(f);
                } else {
                    sittingFlights.add(f);
                }
            }
        }
        return new ResultStruct(sittingFlights, airborneFlights, startTime,
                endTime);
    }

    public static LocalDate parseDepDate(CSVRecord record) {
        int year = Integer.parseInt(record.get(COL_NAME_YEAR));
        int month = Integer.parseInt(record.get(COL_NAME_MONTH));
        int dayOfMonth = Integer.parseInt(record.get(COL_NAME_DAY));
        return LocalDate.of(year, month, dayOfMonth);
    }

    public static ZonedDateTime parseDepZonedDateTime(CSVRecord record)
            throws IOException {
        LocalDate depDate = parseDepDate(record);
        ZoneId timeZone = TimeZoneGetter.getTimeZone(record
                .get(COL_NAME_ORIGIN_APT));

        LocalTime depTime = LocalTime.parse(record.get(COL_NAME_DEP_TIME),
                DEP_TIME_FORMAT);
        return depTime.atDate(depDate).atZone(timeZone);
    }

    public static Duration parseDuration(CSVRecord record) {
        return Duration.ofMinutes((long) Double.parseDouble(record
                .get(COL_NAME_DURATION)));
    }

}
