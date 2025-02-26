package projecteval.PongGame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.Graphics;
import java.util.Random;


public class BallTest {

    private Ball ball;
    private Graphics graphicsMock;
    private Random randomMock;

    @BeforeEach
    void setup() {
        // Use a mock random to control directions
        randomMock = mock(Random.class);
        when(randomMock.nextInt(2)).thenReturn(1); // Force directions to be positive

        ball = new Ball(50, 50, 20, 20) {
            // Override the random with our mock
            { this.random = randomMock; }
        };
        graphicsMock = mock(Graphics.class);
    }

    @Test
    void testConstructorSetsVelocity() {
        // We forced random to return 1, so direction should be positive
        // The code inside Ball uses random.nextInt(2), if it is 0, it changes sign.
        // Here we told it to always be 1, means setXDirection = 1*initialSpeed
        // setYDirection = 1*initialSpeed or something similar
        // But there's a bug in the Ball constructor. We must verify:
        assertTrue(ball.xVelocity != 0, "xVelocity should be set by the constructor");
        assertTrue(ball.yVelocity != 0, "yVelocity should be set by the constructor");
    }

    @Test
    void testSetXDirectionAndMove() {
        ball.setXDirection(5);
        ball.move();
        assertEquals(55, ball.x, "Ball x should move by xVelocity");
    }

    @Test
    void testSetYDirectionAndMove() {
        ball.setYDirection(-3);
        ball.move();
        assertEquals(47, ball.y, "Ball y should move by yVelocity");
    }

    @Test
    void testDraw() {
        ball.draw(graphicsMock);
        verify(graphicsMock).setColor(any());
        verify(graphicsMock).fillOval(ball.x, ball.y, ball.width, ball.height);
    }
}

/******************************************************************************
 * GamePanelTest.java
 ******************************************************************************/
