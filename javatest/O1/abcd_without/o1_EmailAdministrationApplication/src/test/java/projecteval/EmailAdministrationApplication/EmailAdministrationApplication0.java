package projecteval.EmailAdministrationApplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    private final String firstName = "John";
    private final String lastName = "Doe";

    @BeforeEach
    void setup() {
        // Nothing special for now. We will override System.in in each test if needed.
    }

    @Test
    void testConstructorAndDefaults_NoDepartment() {
        // Provide "0" as the department choice
        String input = "0"; 
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        Email email = new Email(firstName, lastName);

        // Verify that email details are assigned properly
        assertEquals("John", email.showInfo().contains("John") ? "John" : null);
        assertTrue(email.showInfo().contains("Doe"), "Should contain last name");
        assertTrue(email.showInfo().contains("Company Email:"), "Should display company email");
        assertEquals(500, email.getMailboxCapacity(), "Default mailbox capacity should be 500mb");
        assertNotNull(email.getPassword(), "Password should be generated");
        // Check that department is empty
        assertTrue(email.showInfo().contains("Department: "),
                "Department line should appear (even if empty)");
    }

    @Test
    void testConstructorAndDefaults_SalesDepartment() {
        // Provide "1" as the department choice
        String input = "1";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        Email email = new Email(firstName, lastName);

        // Check that the department is "sales"
        assertTrue(email.showInfo().contains("Department: sales"), 
                "Department should be sales for input 1");
        assertTrue(email.showInfo().contains("@sales.krishgaur.co"),
                "Email should end with @sales.krishgaur.co");
    }

    @Test
    void testConstructorAndDefaults_DevDepartment() {
        // Provide "2" as the department choice
        String input = "2";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        Email email = new Email(firstName, lastName);

        assertTrue(email.showInfo().contains("Department: dev"), 
                "Department should be dev for input 2");
        assertTrue(email.showInfo().contains("@dev.krishgaur.co"),
                "Email should end with @dev.krishgaur.co");
    }

    @Test
    void testConstructorAndDefaults_AccountDepartment() {
        // Provide "3" as the department choice
        String input = "3";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        Email email = new Email(firstName, lastName);

        assertTrue(email.showInfo().contains("Department: acc"), 
                "Department should be acc for input 3");
        assertTrue(email.showInfo().contains("@acc.krishgaur.co"),
                "Email should end with @acc.krishgaur.co");
    }

    @Test
    void testConstructorAndDefaults_InvalidDepartment() {
        // Provide an invalid department code
        String input = "99";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        Email email = new Email(firstName, lastName);
        
        assertTrue(email.showInfo().contains("***INVALID CHOICE***"), 
                "Should show invalid department choice");
        assertTrue(email.showInfo().contains("@***INVALID CHOICE***.krishgaur.co"),
                "Email domain should reflect invalid department");
    }

    @Test
    void testRandomPasswordLength() {
        // Provide "0" as department just to skip that part
        String input = "0";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        Email email = new Email(firstName, lastName);

        String generatedPass = email.getPassword();
        assertNotNull(generatedPass, "Password should not be null");
        assertEquals(10, generatedPass.length(), "Default password length should be 10");
    }

    @Test
    void testSetMailboxCapacity() {
        // Provide "0" as department
        String input = "0";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);
        Email email = new Email(firstName, lastName);

        email.setMailboxCapacity(1000);

        assertEquals(1000, email.getMailboxCapacity(), "Mailbox capacity should be updated to 1000");
    }

    @Test
    void testSetAlternateEmail() {
        // Provide "0" as department
        String input = "0";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);
        Email email = new Email(firstName, lastName);

        email.setAlternateEmail("alternate@example.com");
        assertEquals("alternate@example.com", email.getAlternateEmail(),
                "Alternate email should be updated correctly");
    }

    @Test
    void testShowInfo() {
        // Provide "1" as department => "sales"
        String input = "1";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        Email email = new Email("Jane", "Smith");
        String info = email.showInfo();

        assertTrue(info.contains("Jane") && info.contains("Smith"), "Should show full name");
        assertTrue(info.contains("sales"), "Should contain department sales");
        assertTrue(info.contains("Mailbox Capacity: 500mb"), "Should show mailbox capacity");
    }
}


// -------------------------------------------------------------------------
// File: MainTest.java
// Location: src/test/java/projecteval/EmailAdministrationApplication/MainTest.java
// -------------------------------------------------------------------------

