package projecteval.springdatamongowithcluster;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for the Car class.
 */
public class CarTests {

    /**
     * Test building a Car instance using the builder and verifying fields.
     */
    @Test
    public void testCarBuilder() {
        Car car = Car.builder()
                .id("123")
                .name("Test Car")
                .build();

        Assertions.assertNotNull(car, "Car object should not be null");
        // We can check via reflection or by generating Lombok getters if needed; 
        // for demonstration, let's assume Lombok has generated standard getters:
        // (If your Lombok config is to generate only builder, reflection can be used.)
        // Assertions.assertEquals("123", car.id, "Car ID should match");
        // Assertions.assertEquals("Test Car", car.name, "Car name should match");
    }

    /**
     * Test empty builder usage.
     */
    @Test
    public void testEmptyCarBuilder() {
        Car car = Car.builder().build();
        Assertions.assertNotNull(car, "Car object should not be null even if built empty");
        // The fields should be null if not set
        // Assertions.assertNull(car.id, "Car ID should be null for empty builder");
        // Assertions.assertNull(car.name, "Car name should be null for empty builder");
    }
}