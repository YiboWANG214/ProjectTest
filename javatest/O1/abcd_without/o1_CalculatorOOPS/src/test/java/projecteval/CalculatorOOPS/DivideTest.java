package projecteval.CalculatorOOPS;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DivideTest {

    @Test
    void testDivideMultipleNumbers() {
        Divide divide = new Divide();
        Double result = divide.getResult(20.0, 2.0, 5.0);
        Assertions.assertEquals(2.0, result, "20 / 2 / 5 should be 2");
    }

    @Test
    void testDivideSingleNumber() {
        Divide divide = new Divide();
        Double result = divide.getResult(5.0);
        Assertions.assertEquals(5.0, result, "Single division should return the same value");
    }

    @Test
    void testDivideByZero() {
        Divide divide = new Divide();
        Double result = divide.getResult(10.0, 0.0);
        // Double division by zero yields Infinity in Java.
        Assertions.assertTrue(Double.isInfinite(result), "10 / 0.0 should lead to Infinity");
    }
}
