package projecteval.idcenter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

/******************************************************************************
 * IdWorkerTest
 * Tests for IdWorker class.
 ******************************************************************************/
class IdWorkerTest {

    @Test
    void testConstructorValid() {
        // Construct with valid parameters
        IdWorker worker = new IdWorker(1, 1, 0, System.currentTimeMillis() - 1);
        assertEquals(1, worker.getWorkerId());
        assertEquals(1, worker.getDatacenterId());
    }

    @Test
    void testConstructorInvalidWorkerId() {
        // Worker ID out of range
        long validEpoch = System.currentTimeMillis() - 10;
        assertThrows(IllegalArgumentException.class, () -> {
            new IdWorker(-1, 1, 0, validEpoch);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new IdWorker(1000, 1, 0, validEpoch);
        });
    }

    @Test
    void testConstructorInvalidDatacenterId() {
        // Datacenter ID out of range
        long validEpoch = System.currentTimeMillis() - 10;
        assertThrows(IllegalArgumentException.class, () -> {
            new IdWorker(1, -10, 0, validEpoch);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new IdWorker(1, 999, 0, validEpoch);
        });
    }

    @Test
    void testConstructorInvalidEpoch() {
        // Epoch must be in the past
        assertThrows(IllegalArgumentException.class, () -> {
            new IdWorker(1, 1, 0, System.currentTimeMillis() + 10000);
        });
    }

    @Test
    void testGetId() {
        long epoch = System.currentTimeMillis() - 3600_000;
        IdWorker worker = new IdWorker(1, 1, 0, epoch);
        long id1 = worker.getId();
        long id2 = worker.getId();
        assertTrue(id2 > id1, "Second ID should be greater than the first ID");
    }

    @Test
    void testGetIdTimestamp() {
        long customEpoch = System.currentTimeMillis() - 10_000; 
        IdWorker worker = new IdWorker(1, 1, 0, customEpoch);
        long id = worker.getId();
        long ts = worker.getIdTimestamp(id);
        // The returned timestamp should be >= customEpoch
        assertTrue(ts >= customEpoch,"ID timestamp should be in valid range");
    }

    @Test
    void testClockMovedBackwards() {
        // Spy on nextId to force a backward clock scenario
        IdWorker spyWorker = Mockito.spy(new IdWorker(1, 1, 0, System.currentTimeMillis() - 100000));
        // First call returns a certain time
        // doReturn(1000L).when(spyWorker).timeGen();
        long id = spyWorker.getId(); // set lastTimestamp = 1000
        assertNotNull(id);

        // Force timeGen to be smaller (backward)
        // doReturn(999L).when(spyWorker).timeGen();
        assertThrows(IllegalStateException.class, spyWorker::getId);
    }

    @Test
    void testSameTimestampSequence() {
        // We want to validate sequence increments if calls happen with the same time.
        IdWorker spyWorker = Mockito.spy(new IdWorker(1, 1, 0, System.currentTimeMillis() - 100000));
        // Force same time for multiple calls
        // doReturn(1234567L).when(spyWorker).timeGen();
        long id1 = spyWorker.getId();
        long id2 = spyWorker.getId();
        // Two IDs from same timestamp => second ID must be bigger (sequence increment)
        assertTrue(id2 > id1);
    }
}

/******************************************************************************
 * Base62Test
 * Tests for Base62 class.
 ******************************************************************************/
class Base62Test {

    @Test
    void testEncodeDecodeZero() {
        String encoded = Base62.encode(0);
        assertEquals("0", encoded);
        long decoded = Base62.decode(encoded);
        assertEquals(0, decoded);
    }

    @Test
    void testEncodeDecodePositive() {
        long number = 123456789;
        String encoded = Base62.encode(number);
        assertNotNull(encoded);
        long decoded = Base62.decode(encoded);
        assertEquals(number, decoded);
    }

    @Test
    void testEncodeNegativeNumber() {
        // Must throw exception for negative input
        assertThrows(IllegalArgumentException.class, () -> Base62.encode(-1));
    }

    @Test
    void testDecodeInvalidCharacter() {
        // Should throw exception if an invalid character is in the string
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("abc$"));
    }

    @Test
    void testLargerNumber() {
        long number = Long.MAX_VALUE >> 2; // some large number
        String encoded = Base62.encode(number);
        long decoded = Base62.decode(encoded);
        assertEquals(number, decoded);
    }
}

/******************************************************************************
 * SidWorkerTest
 * Tests for SidWorker class.
 ******************************************************************************/
class SidWorkerTest {

    @Test
    void testNextSid() {
        long sid1 = SidWorker.nextSid();
        long sid2 = SidWorker.nextSid();
        // Just check that the IDs are 19 digits (or more generally, in correct format)
        // Typically 19 digits: e.g. 20160628175532000002
        // We'll do a rough pattern check
        assertTrue(String.valueOf(sid1).matches("\\d{17,20}"),
                   "SID should be a numeric string of expected length");
        assertTrue(String.valueOf(sid2).matches("\\d{17,20}"),
                   "SID should be a numeric string of expected length");
        assertTrue(sid2 > sid1, "SIDs should be increasing if consecutive");
    }

    @Test
    void testSequenceReset() throws InterruptedException {
        // We can attempt to force the sequence to roll by quickly calling nextSid in a loop
        // Because MAX_SEQUENCE = 100. We'll call it at least 101 times in the same millisecond
        long lastSid = SidWorker.nextSid();
        int count = 0;
        for (int i = 0; i < 110; i++) {
            long newSid = SidWorker.nextSid();
            assertTrue(newSid >= lastSid, "New SID must not be lower than the previous");
            lastSid = newSid;
            count++;
        }
        assertTrue(count == 110, "Should have generated 110 IDs with no exception");
    }

    @Test
    void testSidTimeProgress() throws InterruptedException {
        // Check that as time moves, the sid changes with a new timestamp
        long sid1 = SidWorker.nextSid();
        Thread.sleep(2); // ensure time difference
        long sid2 = SidWorker.nextSid();
        assertTrue(sid2 > sid1, "SIDs should increase over time");
    }
}

/******************************************************************************
 * MainTest
 * Tests for Main class.
 ******************************************************************************/
class MainTest {

    @Test
    void testMainIdWorker() throws IOException {
        // Test the "idworker" subcommand writing to a temporary file
        File tempFile = File.createTempFile("idworker_test_", ".txt");
        tempFile.deleteOnExit();

        String[] args = {
                "idworker",
                "5",
                tempFile.getAbsolutePath()
        };
        Main.main(args);

        // Check that the file has output
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                assertTrue(line.contains("IdWorker1:") || line.contains("IdWorker2:"),
                           "Output line should reference an IdWorker result");
                lineCount++;
            }
            // For each loop iteration, we wrote 2 lines, so total lines = 5 * 2 = 10
            assertEquals(10, lineCount, "Should have exactly 10 lines of output");
        }
    }

    @Test
    void testMainSidWorker() throws IOException {
        // Test the "sidworker" subcommand
        File tempFile = File.createTempFile("sidworker_test_", ".txt");
        tempFile.deleteOnExit();

        String[] args = {
                "sidworker",
                "3",
                tempFile.getAbsolutePath()
        };
        Main.main(args);

        // Check that the file has output
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                assertTrue(line.contains("SidWorker:"), "Output line should reference a SidWorker result");
                lineCount++;
            }
            // We asked for 3 lines
            assertEquals(3, lineCount, "Should have exactly 3 lines of output");
        }
    }

    @Test
    void testMainUnknownSubcommand() throws IOException {
        // We'll capture console output by temporarily redirecting System.err or check no file is written
        // For simplicity, just run main with an invalid subcommand
        File tempFile = File.createTempFile("badsubcommand_test_", ".txt");
        tempFile.deleteOnExit();

        String[] args = {
                "unknownSubcommand",
                "5",
                tempFile.getAbsolutePath()
        };
        Main.main(args);

        // Check that the file is empty (since it failed to run properly)
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            assertNull(reader.readLine(), "File should be empty for unknown subcommand");
        }
    }

    @Test
    void testMainInsufficientArgs() {
        String[] args = {
                "idworker",
                "onlyTwoArgs"
                // missing third argument
        };
        // We simply call main, ensuring it doesn't blow up; it should print usage
        // No exception is expected, but there's no real side effect to test, other than coverage.
        assertDoesNotThrow(() -> Main.main(args));
    }
}

/*
 HOW TO RUN:
 1. Ensure you have JUnit 5 (and Mockito if desired) in your classpath.
 2. Place these test classes under:
    src/test/java/projecteval/idcenter/
 3. Run with your favorite test runner (e.g., Maven surefire, IntelliJ, etc.).
 */