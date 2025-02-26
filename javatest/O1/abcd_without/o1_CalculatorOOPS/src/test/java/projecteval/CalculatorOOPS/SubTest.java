package projecteval.CalculatorOOPS;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SubTest {

    @Test
    void testSubMultipleNumbers() {
        Sub sub = new Sub();
        Double result = sub.getResult(10.0, 2.0, 1.0);
        Assertions.assertEquals(7.0, result, "10 - 2 - 1 should be 7");
    }

    @Test
    void testSubSingleNumber() {
        Sub sub = new Sub();
        Double result = sub.getResult(5.0);
        Assertions.assertEquals(5.0, result, "Single subtraction should just return the initial value");
    }

    @Test
    void testSubNoNumbers() {
        Sub sub = new Sub();
        Double result = sub.getResult();
        Assertions.assertEquals(0.0, result, "No numbers to subtract should default to 0.0");
    }
}
