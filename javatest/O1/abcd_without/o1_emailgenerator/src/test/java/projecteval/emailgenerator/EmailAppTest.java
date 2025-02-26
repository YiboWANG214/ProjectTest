package projecteval.emailgenerator;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/*
  Tests for EmailApp.java.
  These tests focus on verifying that the main method handles inputs and produces expected outputs.
  We use a ByteArrayInputStream to mock user inputs and a ByteArrayOutputStream to capture console outputs.
*/
public class EmailAppTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUpOutputStream() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void restoreOutputStream() {
        System.setOut(standardOut);
    }

    @Test
    @DisplayName("Test main method - scenario: create email, show info, exit immediately")
    void testMainMethodCreateAndExit() {
        /*
           This test simulates user input:
           1) department selection (1 for Sales)
           2) 5 for exit
         */
        String input = 
                // FirstName, LastName
                "John\n" +
                "Doe\n" +
                // Department selection
                "1\n" +
                // Operation code => show info
                "1\n" +
                // Operation code => exit
                "5\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        EmailApp.main(new String[]{});

        String output = outputStreamCaptor.toString();

        // Basic assertions: we expect certain prompts in the output
        assertTrue(output.contains("Enter firstname :"));
        assertTrue(output.contains("Enter Lastname :"));
        assertTrue(output.contains("Kindly ! Enter department for email creation dear John Doe"));
        assertTrue(output.contains("Department:sales"));
        assertTrue(output.contains("Official mail :johndoe.sales@drngpit.ac.in"));
        // showInfo should have the "Name :" and "Official email :"
        assertTrue(output.contains("Name : JohnDoe"));
        // We eventually see the exit statement
        assertTrue(output.contains("Have a great day ahead ! BYE"));
    }


    @Test
    @DisplayName("Test main method - scenario: invalid verification code for password")
    void testMainMethodInvalidVerificationCode() {
        /*
           Scenario flow:
            1) Enter first & last name
            2) Choose department (2 => dev)
            3) Operation code => 3 (to change password)
            4) Enter invalid code
            5) Then enter operation code => 5 (exit)
         */
        String input = 
                // FirstName, LastName
                "Alan\n" +
                "Turing\n" +
                // department => dev
                "2\n" +
                // Operation code => change password
                "3\n" +
                // Verification code => invalid
                "99999\n" +
                // Operation code => exit
                "5\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        EmailApp.main(new String[]{});

        String output = outputStreamCaptor.toString();

        // Check that we saw the correct prompt
        assertTrue(output.contains("Turing"));
        assertTrue(output.contains("Department:dev"));
        assertTrue(output.contains("Enter the verification code :"));
        // We expect a message about invalid verification code
        assertTrue(output.contains("Please Enter valid verification code !!!"));
        // Eventually we see that password was 'updated' message though code is invalid 
        // (the code is placed after the if block, so it always prints "Password updated successfully !!!")
        // This is the logic of the given code. 
        assertTrue(output.contains("Password updated successfully !!!"));
        assertTrue(output.contains("Have a great day ahead ! BYE"));
    }

    @Test
    @DisplayName("Test main method - scenario: change email and show info again")
    void testMainMethodChangeEmail() {
        /*
           Flow:
            1) Enter first, last name
            2) department => 3 => Accounting
            3) operation code => 1 => show info
            4) operation code => 2 => change email to something@drngpit.ac.in
            5) operation code => 1 => show info again
            6) exit
         */
        String input = 
                // First & last
                "Emma\n" +
                "Watson\n" +
                // department => 3 => Accounting
                "3\n" +
                // show info
                "1\n" +
                // change email
                "2\n" +
                // new email prefix
                "myalternate\n" +
                // show info again
                "1\n" +
                // exit
                "5\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        EmailApp.main(new String[]{});

        String output = outputStreamCaptor.toString();
        // Confirm the email was originally set:
        assertTrue(output.contains("Official mail :emmawatson.acc@drngpit.ac.in"));
        // After change email, we expect to see "myalternate@drngpit.ac.in" in the show info
        assertTrue(output.contains("myalternate@drngpit.ac.in"));
        // Confirm final exit
        assertTrue(output.contains("Have a great day ahead ! BYE"));
    }
}



/////////////////////////////////////////////
// File: EmailTest.java
/////////////////////////////////////////////
