package projecteval.libraryApp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class MainTest {

    private final InputStream originalIn = System.in;

    @BeforeEach
    public void setUpStreams() {
        // No-op for now
    }

    @AfterEach
    public void restoreStreams() {
        System.setIn(originalIn);
    }

    @Test
    public void testMainMenu_ExitImmediately() {
        // We'll simulate user input that selects "6" to exit
        String userInput = "6\n";
        System.setIn(new ByteArrayInputStream(userInput.getBytes()));

        // Just ensure no error is thrown
        Main.main(new String[]{});
        assertTrue(true, "Main executed with immediate exit without errors.");
    }

    @Test
    public void testMainMenu_SearchByTitleThenExit() {
        // A sequence: 
        // 1              (search by title) 
        // Some Title     (the search text)
        // 6              (exit)
        String userInput = "1\nSome Title\n6\n";
        System.setIn(new ByteArrayInputStream(userInput.getBytes()));

        // Just ensure no error is thrown
        Main.main(new String[]{});
        assertTrue(true, "Main executed with search by title and exit without errors.");
    }
}