package projecteval.CalculatorOOPS;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class CalculatorTest {

    @Test
    void testCalculatorAddition() {
        // Expression: 5+3 => 8
        String simulatedInput = "5+3";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream backupOut = System.out;
        PrintStream testOut = new PrintStream(outputStream);

        System.setIn(inputStream);
        System.setOut(testOut);

        try {
            Calculator.main(null);
        } finally {
            System.setIn(System.in);
            System.setOut(backupOut);
        }

        String consoleOutput = outputStream.toString().trim();
        Assertions.assertEquals("8.0", consoleOutput, "Calculator with input '5+3' should output 8.0");
    }

    @Test
    void testCalculatorMixedExpression() {
        // Expression: 4*3/2 => (4*3)=12, 12/2=6
        String simulatedInput = "4*3/2";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream backupOut = System.out;
        PrintStream testOut = new PrintStream(outputStream);

        System.setIn(inputStream);
        System.setOut(testOut);

        try {
            Calculator.main(null);
        } finally {
            System.setIn(System.in);
            System.setOut(backupOut);
        }

        String consoleOutput = outputStream.toString().trim();
        Assertions.assertEquals("6.0", consoleOutput, "Calculator with input '4*3/2' should output 6.0");
    }

    @Test
    void testCalculatorSubtraction() {
        // Expression: 10-2 => 8
        String simulatedInput = "10-2";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream backupOut = System.out;
        PrintStream testOut = new PrintStream(outputStream);

        System.setIn(inputStream);
        System.setOut(testOut);

        try {
            Calculator.main(null);
        } finally {
            System.setIn(System.in);
            System.setOut(backupOut);
        }

        String consoleOutput = outputStream.toString().trim();
        Assertions.assertEquals("8.0", consoleOutput, "Calculator with input '10-2' should output 8.0");
    }

    @Test
    void testCalculatorModulus() {
        // Expression: 17%5 => 2
        String simulatedInput = "17%5";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream backupOut = System.out;
        PrintStream testOut = new PrintStream(outputStream);

        System.setIn(inputStream);
        System.setOut(testOut);

        try {
            Calculator.main(null);
        } finally {
            System.setIn(System.in);
            System.setOut(backupOut);
        }

        String consoleOutput = outputStream.toString().trim();
        Assertions.assertEquals("2.0", consoleOutput, "Calculator with input '17%5' should output 2.0");
    }

    @Test
    void testCalculatorMultipleOperators() {
        // Expression: 8+2*3-4 => 8+2=10, 10*3=30, 30-4=26
        String simulatedInput = "8+2*3-4";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream backupOut = System.out;
        PrintStream testOut = new PrintStream(outputStream);

        System.setIn(inputStream);
        System.setOut(testOut);

        try {
            Calculator.main(null);
        } finally {
            System.setIn(System.in);
            System.setOut(backupOut);
        }

        String consoleOutput = outputStream.toString().trim();
        Assertions.assertEquals("26.0", consoleOutput, 
            "Calculator with input '8+2*3-4' should output 26.0");
    }
}
