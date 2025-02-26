package projecteval.CalculatorOOPS;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AddTest {

    @Test
    void testAddMultipleNumbers() {
        Add add = new Add();
        Double result = add.getResult(1.0, 2.0, 3.0);
        Assertions.assertEquals(6.0, result, "1+2+3 should be 6");
    }

    @Test
    void testAddSingleNumber() {
        Add add = new Add();
        Double result = add.getResult(5.0);
        Assertions.assertEquals(5.0, result, "Single addition should return the same value");
    }

    @Test
    void testAddNoNumbers() {
        Add add = new Add();
        Double result = add.getResult();
        Assertions.assertEquals(0.0, result, "When no numbers are provided, sum is 0.0");
    }
}
