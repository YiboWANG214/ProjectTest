package projecteval.CalculatorOOPS;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModulusTest {

    @Test
    void testModulusMultipleNumbers() {
        Modulus modulus = new Modulus();
        Double result = modulus.getResult(20.0, 6.0, 2.0);
        // 20 % 6 = 2, then 2 % 2 = 0
        Assertions.assertEquals(0.0, result, "20 % 6 % 2 should be 0");
    }

    @Test
    void testModulusSingleNumber() {
        Modulus modulus = new Modulus();
        Double result = modulus.getResult(5.0);
        Assertions.assertEquals(5.0, result, "Single modulus should return the same value");
    }

    @Test
    void testModulusByZero() {
        Modulus modulus = new Modulus();
        Double result = modulus.getResult(10.0, 0.0);
        // 10 % 0.0 results in NaN in Java
        Assertions.assertTrue(Double.isNaN(result), "10 % 0.0 should be NaN in Java");
    }
}
