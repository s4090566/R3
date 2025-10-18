package flight;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

public class FlightSearchTest {
    // Fix “today” so date tests are deterministic
    private final Clock fixedClock = Clock.fixed(
            LocalDate.of(2025, 10, 18).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    private FlightSearch fs;

    @BeforeEach
    void setup() {
        fs = new FlightSearch(fixedClock);
    }

    // Helper: verify attributes are still null/zero/false when validation fails
    private void assertUnchanged(FlightSearch f) {
        assertNull(f.getDepartureDate());
        assertNull(f.getReturnDate());
        assertNull(f.getDepartureAirportCode());
        assertNull(f.getDestinationAirportCode());
        assertNull(f.getSeatingClass());
        assertEquals(0, f.getAdultPassengerCount());
        assertEquals(0, f.getChildPassengerCount());
        assertEquals(0, f.getInfantPassengerCount());
        assertFalse(f.isEmergencyRowSeating());
    }

    // ---- Cond 1: total passengers 1..9 ----
    @Test
    void cond1_totalPassengers_zero_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","economy",0,0,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    @Test
    void cond1_totalPassengers_ten_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","economy",9,1,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    // ---- Cond 2: children not in ER / first ----
    @Test
    void cond2_children_emergencyRow_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",true,"20/10/2025","syd","economy",1,1,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    @Test
    void cond2_children_firstClass_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","first",1,1,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    // ---- Cond 3: infants not in ER / business ----
    @Test
    void cond3_infant_emergencyRow_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",true,"20/10/2025","syd","economy",1,0,1);
        assertFalse(ok); assertUnchanged(fs);
    }

    @Test
    void cond3_infant_business_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","business",1,0,1);
        assertFalse(ok); assertUnchanged(fs);
    }

    // ---- Cond 4: ≤ 2 children per adult ----
    @Test
    void cond4_children_per_adult_boundary_valid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","economy",2,4,0);
        assertTrue(ok); assertEquals(4, fs.getChildPassengerCount());
    }

    @Test
    void cond4_children_too_many_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","economy",1,3,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    // ---- Cond 5: ≤ 1 infant per adult ----
    @Test
    void cond5_infant_per_adult_boundary_valid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","economy",2,0,2);
        assertTrue(ok); assertEquals(2, fs.getInfantPassengerCount());
    }

    @Test
    void cond5_infant_too_many_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","economy",1,0,2);
        assertFalse(ok); assertUnchanged(fs);
    }

    // ---- Cond 6: departure not in the past ----
    @Test
    void cond6_departure_in_past_invalid() {
        boolean ok = fs.runFlightSearch("17/10/2025","mel",false,"20/10/2025","syd","economy",1,0,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    @Test
    void cond6_departure_today_valid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"19/10/2025","syd","economy",1,0,0);
        assertTrue(ok); assertEquals("18/10/2025", fs.getDepartureDate());
    }

    // ---- Cond 7: strict date validation ----
    @Test
    void cond7_invalid_date_combo_invalid() {
        assertFalse(fs.runFlightSearch("31/04/2026","mel",false,"02/05/2026","syd","economy",1,0,0));
        assertFalse(fs.runFlightSearch("29/02/2026","mel",false,"01/03/2026","syd","economy",1,0,0));
    }

    @Test
    void cond7_leap_year_valid() {
        assertTrue(fs.runFlightSearch("29/02/2028","mel",false,"01/03/2028","syd","economy",1,0,0));
    }

    // ---- Cond 8: return not before departure ----
    @Test
    void cond8_return_before_departure_invalid() {
        boolean ok = fs.runFlightSearch("20/10/2025","mel",false,"19/10/2025","syd","economy",1,0,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    @Test
    void cond8_same_day_return_valid() {
        boolean ok = fs.runFlightSearch("20/10/2025","mel",false,"20/10/2025","syd","economy",1,0,0);
        assertTrue(ok);
    }

    // ---- Cond 9: seating class valid set ----
    @Test
    void cond9_unknown_class_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","syd","ultra",1,0,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    // ---- Cond 10: emergency row only in economy ----
    @Test
    void cond10_emergency_in_business_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",true,"20/10/2025","syd","business",1,0,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    // ---- Cond 11: airports list + not same ----
    @Test
    void cond11_invalid_airport_code_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","zzz",false,"20/10/2025","syd","economy",1,0,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    @Test
    void cond11_same_departure_and_destination_invalid() {
        boolean ok = fs.runFlightSearch("18/10/2025","mel",false,"20/10/2025","mel","economy",1,0,0);
        assertFalse(ok); assertUnchanged(fs);
    }

    // ---- All-valid smoke cases ----
    @Test
    void allValid_economy_family_no_emergency() {
        boolean ok = fs.runFlightSearch("25/10/2025","mel",false,"05/11/2025","pvg","economy",2,3,1);
        assertTrue(ok);
        assertEquals("mel", fs.getDepartureAirportCode());
        assertEquals("pvg", fs.getDestinationAirportCode());
        assertEquals("economy", fs.getSeatingClass());
        assertFalse(fs.isEmergencyRowSeating());
    }

    @Test
    void allValid_premium_economy_adults_only() {
        assertTrue(fs.runFlightSearch("20/12/2025","syd",false,"30/12/2025","lax","premium economy",2,0,0));
    }

    @Test
    void allValid_first_no_children() {
        assertTrue(fs.runFlightSearch("10/01/2026","mel",false,"25/01/2026","cdg","first",1,0,0));
    }

    @Test
    void allValid_economy_emergency_no_kids_or_infants() {
        assertTrue(fs.runFlightSearch("18/10/2025","del",true,"28/10/2025","doh","economy",1,0,0));
    }
}
