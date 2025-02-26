package projecteval.PongGame;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;


public class PongGameTest {

    @Test
    void testMainMethod() {
        // Main should not throw exceptions on a basic run
        assertDoesNotThrow(() -> PongGame.main(new String[]{}));
    }
}

/******************************************************************************
 * BallTest.java
 ******************************************************************************/
