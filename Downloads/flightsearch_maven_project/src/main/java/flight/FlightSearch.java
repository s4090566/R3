package flight;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Set;

/**
 * FlightSearch implementation that validates inputs against the 11 rules.
 * Attributes are updated only when all validations pass.
 * Includes getters used by the JUnit 5 tests.
 */
public class FlightSearch {
    private String  departureDate;
    private String  departureAirportCode;
    private boolean emergencyRowSeating;
    private String  returnDate;
    private String  destinationAirportCode;
    private String  seatingClass;
    private int     adultPassengerCount;
    private int     childPassengerCount;
    private int     infantPassengerCount;

    private final Clock clock; // for deterministic tests

    public FlightSearch() { this(Clock.systemDefaultZone()); }
    public FlightSearch(Clock clock) { this.clock = clock; }

    // === Getters required by tests ===
    public String getDepartureDate() { return departureDate; }
    public String getDepartureAirportCode() { return departureAirportCode; }
    public boolean isEmergencyRowSeating() { return emergencyRowSeating; }
    public String getReturnDate() { return returnDate; }
    public String getDestinationAirportCode() { return destinationAirportCode; }
    public String getSeatingClass() { return seatingClass; }
    public int getAdultPassengerCount() { return adultPassengerCount; }
    public int getChildPassengerCount() { return childPassengerCount; }
    public int getInfantPassengerCount() { return infantPassengerCount; }

    private static final Set<String> VALID_CLASSES = Set.of("economy", "premium economy", "business", "first");
    private static final Set<String> VALID_AIRPORTS = Set.of("syd","mel","lax","cdg","del","pvg","doh");
    private static final DateTimeFormatter DMY_STRICT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public boolean runFlightSearch(String departureDate, String departureAirportCode, boolean emergencyRowSeating,
                                   String returnDate, String destinationAirportCode, String seatingClass,
                                   int adultPassengerCount, int childPassengerCount, int infantPassengerCount) {

        // null guard
        if (departureDate == null || returnDate == null ||
            departureAirportCode == null || destinationAirportCode == null || seatingClass == null) {
            return false;
        }

        // classes + emergency rule
        if (!VALID_CLASSES.contains(seatingClass)) return false;                 // Cond 9
        if (emergencyRowSeating && !"economy".equals(seatingClass)) return false; // Cond 10

        // airports
        if (!VALID_AIRPORTS.contains(departureAirportCode) || !VALID_AIRPORTS.contains(destinationAirportCode)) return false; // Cond 11
        if (departureAirportCode.equals(destinationAirportCode)) return false;   // Cond 11 (not same)

        // passenger totals
        if (adultPassengerCount < 0 || childPassengerCount < 0 || infantPassengerCount < 0) return false;
        int total = adultPassengerCount + childPassengerCount + infantPassengerCount;
        if (total < 1 || total > 9) return false;                                // Cond 1

        // children rules
        if (childPassengerCount > 0) {
            if (emergencyRowSeating) return false;                               // Cond 2
            if ("first".equals(seatingClass)) return false;                      // Cond 2
            if (childPassengerCount > 2 * adultPassengerCount) return false;     // Cond 4
        }

        // infant rules
        if (infantPassengerCount > 0) {
            if (emergencyRowSeating) return false;                               // Cond 3
            if ("business".equals(seatingClass)) return false;                   // Cond 3
            if (infantPassengerCount > adultPassengerCount) return false;        // Cond 5
        }

        // dates
        LocalDate dep, ret;
        try {
            dep = LocalDate.parse(departureDate, DMY_STRICT);
            ret = LocalDate.parse(returnDate, DMY_STRICT);                       // Cond 7
        } catch (Exception e) { return false; }

        LocalDate today = LocalDate.now(clock);
        if (dep.isBefore(today)) return false;                                    // Cond 6
        if (ret.isBefore(dep)) return false;                                      // Cond 8

        // assign atomically after all checks
        this.departureDate = departureDate;
        this.departureAirportCode = departureAirportCode;
        this.emergencyRowSeating = emergencyRowSeating;
        this.returnDate = returnDate;
        this.destinationAirportCode = destinationAirportCode;
        this.seatingClass = seatingClass;
        this.adultPassengerCount = adultPassengerCount;
        this.childPassengerCount = childPassengerCount;
        this.infantPassengerCount = infantPassengerCount;
        return true;
    }
}
