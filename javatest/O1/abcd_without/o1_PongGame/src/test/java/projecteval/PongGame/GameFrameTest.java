package projecteval.PongGame;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


public class GameFrameTest {

    @Test
    void testGameFrameInitialization() {
        GameFrame frame = new GameFrame();
        assertNotNull(frame.panel, "GamePanel should be initialized");
        assertFalse(frame.isResizable(), "Frame should not be resizable");
        assertEquals("Pong Game", frame.getTitle(), "Title should match 'Pong Game'");
        assertTrue(frame.isVisible(), "Frame should be visible");
        frame.dispose();
    }
}

/******************************************************************************
 * PaddleTest.java
 ******************************************************************************/
