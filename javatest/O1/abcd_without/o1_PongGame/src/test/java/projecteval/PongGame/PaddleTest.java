package projecteval.PongGame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.*;
import java.awt.event.KeyEvent;


public class PaddleTest {
    
    private Paddle paddle;
    private Graphics graphicsMock;

    @BeforeEach
    void setup() {
        paddle = new Paddle(10, 20, 25, 100, 1);
        graphicsMock = mock(Graphics.class);
    }

    @Test
    void testConstructorSetsFields() {
        assertEquals(10, paddle.x, "Paddle x should be set by constructor");
        assertEquals(20, paddle.y, "Paddle y should be set by constructor");
        assertEquals(25, paddle.width, "Paddle width should be set by constructor");
        assertEquals(100, paddle.height, "Paddle height should be set by constructor");
        assertEquals(1, paddle.id, "Paddle id should match constructor argument");
    }

    @Test
    void testKeyPressedW() {
        KeyEvent event = mock(KeyEvent.class);
        when(event.getKeyCode()).thenReturn(KeyEvent.VK_W);

        paddle.keyPressed(event);
        assertEquals(-paddle.speed, paddle.yVelocity, "Paddle yVelocity should be set to -speed on W key press");
    }

    @Test
    void testKeyReleasedW() {
        KeyEvent event = mock(KeyEvent.class);
        when(event.getKeyCode()).thenReturn(KeyEvent.VK_W);

        paddle.keyReleased(event);
        assertEquals(0, paddle.yVelocity, "Paddle yVelocity should be 0 after releasing W key");
    }

    @Test
    void testMove() {
        int initialY = paddle.y;
        paddle.setYDirection(5);
        paddle.move();
        assertEquals(initialY + 5, paddle.y, "Paddle y should have increased by 5");
    }

    @Test
    void testDraw() {
        paddle.draw(graphicsMock);
        verify(graphicsMock).setColor(Color.BLUE);
        verify(graphicsMock).fillRect(paddle.x, paddle.y, paddle.width, paddle.height);
    }
}

/******************************************************************************
 * PongGameTest.java
 ******************************************************************************/
