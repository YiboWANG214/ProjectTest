package projecteval.heap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Set;


/**
 * Unit tests for the HeapPerformanceTest class.
 */
public class HeapPerformanceTestTest {

    private HeapPerformanceTest performanceTestSmall;

    @BeforeEach
    public void setup() {
        // Use a small number of elements to ensure quick test
        performanceTestSmall = new HeapPerformanceTest(10);
    }

    @Test
    public void testPerformanceTestReturnsValidMap() {
        HashMap<String, Long> result = performanceTestSmall.test();
        assertNotNull(result, "The test method should return a non-null result.");
        // Ensure it contains all the relevant keys
        Set<String> keys = result.keySet();
        assertTrue(keys.contains("Fibonacci Heap Insertion Time"), "Should contain Fibonacci insertion time key.");
        assertTrue(keys.contains("Leftist Heap Insertion Time"), "Should contain Leftist insertion time key.");
        assertTrue(keys.contains("Fibonacci Heap Deletion Time"), "Should contain Fibonacci deletion time key.");
        assertTrue(keys.contains("Leftist Heap Deletion Time"), "Should contain Leftist deletion time key.");
        assertTrue(keys.contains("Fibonacci Heap Merge Time"), "Should contain Fibonacci merge time key.");
        assertTrue(keys.contains("Leftist Heap Merge Time"), "Should contain Leftist merge time key.");

        // Ensure all values are >= 0
        for (Long timeValue : result.values()) {
            assertTrue(timeValue >= 0, "Time measurements should be non-negative.");
        }
    }

    @Test
    public void testMainMethod() {
        // We won't fully execute main with args to avoid undesired console output in a typical CI.
        // Instead, we can check that calling main with valid arguments doesn't throw exceptions.
        try {
            HeapPerformanceTest.main(new String[]{"5"});
        } catch (Exception e) {
            fail("Calling main should not throw an exception. " + e.getMessage());
        }
    }
}
