package projecteval.EmailAdministrationApplication;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void testMainMethod() {
        // Prepare input for the main method: firstName=Alan, LastName=Turing, then choose department 2 (dev).
        String input = "Alan\nTuring\n2\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        // Capture the output printed to the console
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        // Run the main method
        String[] args = {};
        Main.main(args);

        String output = outContent.toString();

        // Check that essential parts of the flow are present in the console output
        assertTrue(output.contains("Enter your First Name:"), 
                "Should prompt for the first name");
        assertTrue(output.contains("Enter your Last Name:"), 
                "Should prompt for the last name");
        assertTrue(output.contains("Department Codes"), 
                "Should prompt with department codes");
        assertTrue(output.contains("Email Created: Alan Turing"), 
                "Should display that email was created");
        assertTrue(output.contains("Department: dev"), 
                "Should reflect that dev department was chosen");
    }
}

//
// Notes/Tips for usage:
//
// 1. These tests use System.setIn(...) and System.setOut(...) to redirect standard IO. 
//    This is a common approach for quickly capturing console input/output for testing.
//
// 2. If you wish to use pure mocking for Scanner, you would need to redesign the Email
//    class to inject a Scanner (or input provider) instead of constructing it directly.
//
// 3. Use “mvn test” (Maven) or “gradle test” (Gradle) to compile and run these tests.
//
// 4. These tests aim to cover the constructor logic, password generation, 
//    department selection, email address formation, setter methods, and the main method. 
//    Together, they should give you a high coverage rate for the provided classes. 
//
// End of file.