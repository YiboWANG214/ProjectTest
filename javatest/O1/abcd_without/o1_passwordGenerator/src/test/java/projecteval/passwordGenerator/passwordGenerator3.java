package projecteval.passwordGenerator;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Scanner;

/*
  Note: Testing main(...) directly can be tricky because it exits the program
  or engages in a loop reading from System.in. We can test the core logic by
  mocking the Scanner and verifying calls flow as expected in mainLoop().
*/

@ExtendWith(MockitoExtension.class)
class MainTest {

    @Mock
    Scanner scannerMock;

    @InjectMocks
    Generator generator;

    @Test
    void testMainLoopQuitImmediately() {
        // Force the userOption to be "4" so it should exit
        when(scannerMock.next()).thenReturn("4");

        generator.mainLoop();
        // No exception => pass
        verify(scannerMock, atLeastOnce()).next();
    }

    @Test
    void testMainLoopGeneratePasswordThenQuit() {
        // "1" => requestPassword, "4" => quit
        // the requestPassword method reads with nextLine() calls as well
        // so let's provide minimal stubs
        when(scannerMock.next()).thenReturn("1", "4");  // userOption
        when(scannerMock.nextLine()).thenReturn("yes", "yes", "yes", "yes", "8"); 
        // "yes" for all character sets, length=8

        generator.mainLoop();

        // verify at least some interactions
        verify(scannerMock, atLeast(2)).next();
        verify(scannerMock, atLeast(5)).nextLine(); 
    }

    @Test
    void testMainLoopCheckPasswordThenQuit() {
        when(scannerMock.next()).thenReturn("2", "4"); // user option
        when(scannerMock.nextLine()).thenReturn("MySecretPassword"); // input for checkPassword
        generator.mainLoop();
        verify(scannerMock, atLeast(1)).nextLine();
    }
}