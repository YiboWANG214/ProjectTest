package projecteval.CalculatorOOPS;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MultiplyTest {

    @Test
    void testMultiplyMultipleNumbers() {
        Multiply multiply = new Multiply();
        Double result = multiply.getResult(2.0, 3.0, 4.0);
        Assertions.assertEquals(24.0, result, "2 * 3 * 4 should be 24");
    }

    @Test
    void testMultiplySingleNumber() {
        Multiply multiply = new Multiply();
        Double result = multiply.getResult(5.0);
        Assertions.assertEquals(5.0, result, "Single multiplication should return the same value");
    }

    @Test
    void testMultiplyNoNumbers() {
        Multiply multiply = new Multiply();
        Double result = multiply.getResult();
        Assertions.assertEquals(1.0, result, "No numbers to multiply should default to 1.0");
    }

    @Test
    void testMultiplyWithZero() {
        Multiply multiply = new Multiply();
        Double result = multiply.getResult(2.0, 0.0, 4.0);
        Assertions.assertEquals(0.0, result, "Any multiplication by zero should be 0");
    }
}
