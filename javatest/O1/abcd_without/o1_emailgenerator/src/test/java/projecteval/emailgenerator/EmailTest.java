package projecteval.emailgenerator;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/*
   Tests for Email.java.
   We aim to cover:
     - Constructor
     - setDepartment(), getDepartment()
     - setEmail(), getEmail()
     - setPassword(), getPassword()
     - setFirstName(), getFirstName()
     - getVcode()
     - getDept(String dep) => returns full dept name
     - showInfo() => includes name, email, department
   We will mock the user input for setDepartment() with ByteArrayInputStream.
   We also mock random generation if needed. 
*/
public class EmailTest {

    private InputStream standardIn;

    @BeforeEach
    void saveSystemIn() {
        standardIn = System.in;
    }

    @AfterEach
    void restoreSystemIn() {
        System.setIn(standardIn);
    }

    @Test
    @DisplayName("Test constructor - with department = 1 => sales")
    void testConstructorSales() {
        // We must feed "1" for the department
        String input = "1\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Email email = new Email("John", "Doe");
        assertNotNull(email);
        assertEquals("John", email.getFirstName());
        // Because we typed "1", we expect "sales"
        assertEquals("sales", email.getDepartment());
        assertTrue(email.getEmail().contains("johndoe.sales@drngpit.ac.in"));
        assertNotNull(email.getPassword());
        assertEquals(8, email.getPassword().length());
        assertNotNull(email.getVcode());
        assertEquals(5, email.getVcode().length());
    }

    @Test
    @DisplayName("Test constructor - with department = 2 => dev")
    void testConstructorDev() {
        // For coverage, let's test department 2
        String input = "2\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Email email = new Email("Ada", "Lovelace");
        assertEquals("Ada", email.getFirstName());
        assertEquals("dev", email.getDepartment());
        assertTrue(email.getEmail().contains("adalovelace.dev@drngpit.ac.in"));
    }

    @Test
    @DisplayName("Test constructor - with department = 3 => acc")
    void testConstructorAcc() {
        // For coverage, let's test department 3
        String input = "3\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Email email = new Email("Mark", "Zuckerberg");
        assertEquals("acc", email.getDepartment());
        assertTrue(email.getEmail().contains("markzuckerberg.acc@drngpit.ac.in"));
    }

    @Test
    @DisplayName("Test constructor - with department outside of 1,2,3 => empty department")
    void testConstructorNoDepartment() {
        // For coverage, let's test invalid dept => we get empty string
        String input = "9\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Email email = new Email("Elon", "Musk");
        assertEquals("", email.getDepartment());
        assertTrue(email.getEmail().contains("elonmusk..@drngpit.ac.in")); 
        // Notice there's an extra '.' => name.toLowerCase() + '.' + department
    }

    @Test
    @DisplayName("Test setDepartment() and getDepartment()")
    void testSetGetDepartment() {
        // We do not rely on constructor's prompt here, so let's do a manual set
        Email email = Mockito.mock(Email.class, Mockito.withSettings()
                .useConstructor("Test", "User")
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        email.setDepartment("acc");
        assertEquals("acc", email.getDepartment());
    }

    @Test
    @DisplayName("Test setEmail() and getEmail()")
    void testSetGetEmail() {
        Email email = Mockito.mock(Email.class, Mockito.withSettings()
                .useConstructor("Test", "User")
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        email.setEmail("testuser.sales@drngpit.ac.in");
        assertEquals("testuser.sales@drngpit.ac.in", email.getEmail());
    }

    @Test
    @DisplayName("Test setFirstName() and getFirstName()")
    void testSetGetFirstName() {
        Email email = Mockito.mock(Email.class, Mockito.withSettings()
                .useConstructor("Jane", "Doe")
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        assertEquals("Jane", email.getFirstName());
        email.setFirstName("Janet");
        assertEquals("Janet", email.getFirstName());
    }

    @Test
    @DisplayName("Test setPassword() and getPassword()")
    void testSetGetPassword() {
        Email email = Mockito.mock(Email.class, Mockito.withSettings()
                .useConstructor("Alice", "Wonderland")
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));
        email.setPassword("secret");
        assertEquals("secret", email.getPassword());
    }

    @Test
    @DisplayName("Test getVcode()")
    void testGetVcode() {
        // The constructor generates a vcode automatically
        // We can just confirm it is a non-null 5-digit code.
        String input = "1\n"; // department
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Email email = new Email("Foo", "Bar");
        String vcode = email.getVcode();
        assertNotNull(vcode);
        assertEquals(5, vcode.length());
    }

    @Test
    @DisplayName("Test getDept() with dev, acc, sales, or unknown")
    void testGetDept() {
        Email email = Mockito.mock(Email.class, Mockito.withSettings()
                .useConstructor("Dummy", "User")
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));
        assertEquals("Developers", email.getDept("dev"));
        assertEquals("Accounts", email.getDept("acc"));
        assertEquals("Sales", email.getDept("sales"));
        assertEquals("", email.getDept("random"));
    }

    @Test
    @DisplayName("Test showInfo()")
    void testShowInfo() {
        String input = "1\n"; // department => sales
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Email email = new Email("Test", "User");
        String info = email.showInfo();
        /*
           showInfo() => 
             "Name : " + name + 
             "\nOfficial email : " + email + 
             "\nDepartment : " + getDept(department);
         */
        assertTrue(info.contains("Name : TestUser"));
        assertTrue(info.contains("Official email : testuser.sales@drngpit.ac.in"));
        assertTrue(info.contains("Department : Sales"));
    }
}