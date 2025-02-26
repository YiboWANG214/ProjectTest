package projecteval.Train.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.Train.Driver;
import projecteval.Train.Train;
import projecteval.Train.TrainStation;

/*
 * ====================================================
 *                   TrainTest
 * ====================================================
 */
class TrainTest {

    @Test
    void testConstructorAndGetters() {
        Train train = new Train("Aarhus", "Copenhagen", 100);

        assertEquals("Aarhus", train.getDeparture(), "Departure should be Aarhus");
        assertEquals("Copenhagen", train.getDestiantion(), "Destination should be Copenhagen");
        assertEquals(100, train.getPrice(), "Price should be 100");
    }

    @Test
    void testToString() {
        Train train = new Train("Herning", "Berlin", 200);
        String expected = "From Herning to Berlin for 200 DKK";
        assertEquals(expected, train.toString(), "toString() does not match expected format");
    }

    @Test
    void testCompareToDifferentDeparture() {
        Train train1 = new Train("Aarhus", "Copenhagen", 50);
        Train train2 = new Train("Berlin", "Copenhagen", 50);

        // Alphabetical: "Aarhus" < "Berlin", so train1 should come before train2
        assertTrue(train1.compareTo(train2) < 0, "Train1 should be less than Train2 when comparing departures");
    }

    @Test
    void testCompareToSameDepartureDifferentPrice() {
        Train train1 = new Train("Aarhus", "Copenhagen", 50);
        Train train2 = new Train("Aarhus", "Odense", 100);

        // Same departure, compare by price: 50 < 100, so train1 < train2
        assertTrue(train1.compareTo(train2) < 0, "Train1 should be less than Train2 when same departure but cheaper");
    }

    @Test
    void testCompareToSameDepartureSamePrice() {
        Train train1 = new Train("Aarhus", "Copenhagen", 50);
        Train train2 = new Train("Aarhus", "Odense", 50);

        // Same departure, same price => compareTo should return 0 or a negligible difference
        assertEquals(0, train1.compareTo(train2), "Trains with same departure and same price should be considered equal");
    }
}

/*
 * ====================================================
 *                 TrainStationTest
 * ====================================================
 */
@ExtendWith(MockitoExtension.class)
class TrainStationTest {

    @Mock
    private Train mockTrain1;

    @Mock
    private Train mockTrain2;

    @Mock
    private Train mockTrain3;

    @InjectMocks
    private TrainStation station; // city will be null before we set it in a test, or we build a new one

    @BeforeEach
    void setup() {
        // We'll construct a new station each time with city = "Herning"
        station = new TrainStation("Herning");
    }

    @Test
    void testAddAndConnectTrains() {
        when(mockTrain1.getDeparture()).thenReturn("Herning");
        when(mockTrain1.getDestiantion()).thenReturn("Copenhagen");
        when(mockTrain2.getDeparture()).thenReturn("Aarhus");
        when(mockTrain2.getDestiantion()).thenReturn("Herning");
        when(mockTrain3.getDeparture()).thenReturn("Odense");
        when(mockTrain3.getDestiantion()).thenReturn("Berlin");

        station.addTrain(mockTrain1);
        station.addTrain(mockTrain2);
        station.addTrain(mockTrain3);

        // connectingTrains() = number of trains with departure or destination "Herning"
        // mockTrain1 -> departure = Herning, mockTrain2 -> destination = Herning
        // mockTrain3 neither
        assertEquals(2, station.connectingTrains(),
                "Should find 2 trains connecting to Herning");
        verify(mockTrain1, atLeastOnce()).getDeparture();
        verify(mockTrain2, atLeastOnce()).getDestiantion();
        verifyNoMoreInteractions(mockTrain3);
    }

    @Test
    void testCheapTrainTo() {
        when(mockTrain1.getDestiantion()).thenReturn("Copenhagen");
        when(mockTrain1.getPrice()).thenReturn(500);

        when(mockTrain2.getDestiantion()).thenReturn("Copenhagen");
        when(mockTrain2.getPrice()).thenReturn(200);

        when(mockTrain3.getDestiantion()).thenReturn("Berlin");
        when(mockTrain3.getPrice()).thenReturn(100);

        station.addTrain(mockTrain1);
        station.addTrain(mockTrain2);
        station.addTrain(mockTrain3);

        // The cheapest train to "Copenhagen" should be mockTrain2 (price 200)
        Train cheapest = station.cheapTrainTo("Copenhagen");
        assertEquals(mockTrain2, cheapest, "Cheapest train to Copenhagen should be mockTrain2");
    }

    @Test
    void testTrainsFrom() {
        when(mockTrain1.getDeparture()).thenReturn("Aarhus");
        when(mockTrain1.getDestiantion()).thenReturn("Herning");
        when(mockTrain2.getDeparture()).thenReturn("Aarhus");
        when(mockTrain2.getDestiantion()).thenReturn("Copenhagen");
        when(mockTrain3.getDeparture()).thenReturn("Herning");
        when(mockTrain3.getDestiantion()).thenReturn("Aarhus");

        station.addTrain(mockTrain1);
        station.addTrain(mockTrain2);
        station.addTrain(mockTrain3);

        // We look for trains from "Aarhus" going TO "Herning" (station's city)
        List<Train> fromList = station.trainsFrom("Aarhus");
        assertEquals(1, fromList.size(), "Should find exactly 1 train from Aarhus to Herning");
        assertTrue(fromList.contains(mockTrain1), "mockTrain1 is the only match");
    }

    @Test
    void testCheapTrain() {
        // We only consider trains that DEPART from station's city "Herning" -> so mockTrain1 is relevant
        when(mockTrain1.getDeparture()).thenReturn("Herning");
        when(mockTrain1.getDestiantion()).thenReturn("Copenhagen");
        when(mockTrain1.getPrice()).thenReturn(300);

        when(mockTrain2.getDeparture()).thenReturn("Herning");
        when(mockTrain2.getDestiantion()).thenReturn("Copenhagen");
        when(mockTrain2.getPrice()).thenReturn(100);

        when(mockTrain3.getDeparture()).thenReturn("Aarhus");
        when(mockTrain3.getDestiantion()).thenReturn("Copenhagen");
        when(mockTrain3.getPrice()).thenReturn(50);

        station.addTrain(mockTrain1);
        station.addTrain(mockTrain2);
        station.addTrain(mockTrain3);

        // cheapTrain("Copenhagen") -> among those departing "Herning", mockTrain2 is cheapest (100)
        Train cheapest = station.cheapTrain("Copenhagen");
        assertEquals(mockTrain2, cheapest,
                "Cheapest train from Herning to Copenhagen should be mockTrain2");
    }

    @Test
    void testPrintTrainStation() {
        // We'll capture System.out
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        when(mockTrain1.getDeparture()).thenReturn("Aaarhus");
        when(mockTrain1.getPrice()).thenReturn(300);

        when(mockTrain2.getDeparture()).thenReturn("Aaarhus");
        when(mockTrain2.getPrice()).thenReturn(100);

        // Add them so we can trigger printTrainStation(), which also sorts
        station.addTrain(mockTrain1);
        station.addTrain(mockTrain2);

        station.printTrainStation(); // prints out

        System.setOut(originalOut);
        String output = outContent.toString();

        // Basic checks that something was printed out
        assertTrue(output.contains("The trainstaion in Herning has following trains"),
                "Should print train station header");
        // After sorting, mockTrain2 has same departure as mockTrain1 but cheaper price, so it is printed first
        // We won't do an exact string check but we confirm some presence:
        assertTrue(output.contains("Aaarhus"), "Should show the departure Aaarhus in list");
        verify(mockTrain1, atLeastOnce()).getDeparture();
        verify(mockTrain1, atLeastOnce()).getPrice();
        verify(mockTrain2, atLeastOnce()).getPrice();
    }

}

/*
 * ====================================================
 *                   DriverTest
 * ====================================================
 */
class DriverTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testDriverMethod() {
        // Simply call the test() method and ensure it runs without error
        // and prints something to the console.
        Driver.test();
        String output = outContent.toString();

        assertNotNull(output, "Output should not be null after calling Driver.test()");
        // Basic checks on major text lines:
        assertTrue(output.contains("Opgave 3:"), "Should contain 'Opgave 3' section");
        assertTrue(output.contains("Opgave 8:"), "Should contain 'Opgave 8' section");
        assertTrue(output.contains("Opgave 9:"), "Should contain 'Opgave 9' section");
        assertTrue(output.contains("Opgave 10:"), "Should contain 'Opgave 10' section");
        assertTrue(output.contains("Opgave 11:"), "Should contain 'Opgave 11' section");
        assertTrue(output.contains("Opgave 12:"), "Should contain 'Opgave 12' section");
    }
}