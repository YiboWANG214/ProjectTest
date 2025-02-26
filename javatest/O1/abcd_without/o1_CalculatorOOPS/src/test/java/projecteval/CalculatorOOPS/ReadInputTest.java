package projecteval.CalculatorOOPS;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ReadInputTest {

    @Test
    void testReadValidInput() {
        String simulatedInput = "4*3/2";
        InputStream backupIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
            String inputLine = ReadInput.read();
            Assertions.assertEquals(simulatedInput, inputLine, "Should read the same expression that was input");
        } finally {
            System.setIn(backupIn);
        }
    }
}
