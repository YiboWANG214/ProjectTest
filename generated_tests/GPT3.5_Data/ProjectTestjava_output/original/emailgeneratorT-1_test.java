```java
# EmailAppTest.java

package projecttest.emailgenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailAppTest {

    @Mock
    Scanner scanner;

    EmailApp emailApp;

    @BeforeEach
    void setUp() {
        emailApp = new EmailApp();
    }

    @Test
    void testChangeEmail() {
        when(scanner.nextLine()).thenReturn("testprefix");
        System.setIn(new ByteArrayInputStream("2\n".getBytes()));
        emailApp.main(new String[]{});
        // Add assertion here
    }

    @Test
    void testChangePassword() {
        when(scanner.nextLine()).thenReturn("verificationcode", "newpassword");
        System.setIn(new ByteArrayInputStream("3\n".getBytes()));
        emailApp.main(new String[]{});
        // Add assertion here
    }

    @Test
    void testDisclosePassword() {
        when(scanner.nextLine()).thenReturn("verificationcode");
        System.setIn(new ByteArrayInputStream("4\n".getBytes()));
        emailApp.main(new String[]{});
        // Add assertion here
    }
}

# EmailTest.java

package projecttest.emailgenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmailTest {

    Email email;

    @BeforeEach
    void setUp() {
        email = new Email("John", "Doe");
    }

    @Test
    void testSetPassword() {
        email.setPassword("newpassword");
        assertEquals("newpassword", email.getPassword());
    }

    @Test
    void testSetDepartment() {
        email.setDepartment("dev");
        assertEquals("dev", email.getDepartment());
    }

    @Test
    void testSetEmail() {
        email.setEmail("testemail@company.com");
        assertEquals("testemail@company.com", email.getEmail());
    }

    @Test
    void testShowInfo() {
        String expectedInfo = "Name : JohnDoe\nOfficial email : johndoe.dev@drngpit.ac.in\nDepartment : Developers";
        assertEquals(expectedInfo, email.showInfo());
    }
}
```