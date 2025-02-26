package projecteval.PongGame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Random;


public class GamePanelTest {

    private GamePanel panel;
    private Graphics graphicsMock;
    private Random randomMock;

    @BeforeEach
    void setup() {
        panel = new GamePanel();
        graphicsMock = mock(Graphics.class);
        randomMock = mock(Random.class);
    }

    @Test
    void testNewBall() {
        // Force nextInt to return a constant
        when(randomMock.nextInt(anyInt())).thenReturn(100);
        
        // We'll override the panel's random with our mock for controlled testing
        panel.random = randomMock;
        panel.newBall();

        assertNotNull(panel.ball, "Ball should be created by newBall()");
        // Verify it has correct position
        assertEquals(GamePanel.GAME_WIDTH / 2 - GamePanel.BALL_DIAMETER / 2, panel.ball.x, 
                     "Ball x should be centered by newBall()");
    }

    @Test
    void testNewPaddles() {
        panel.newPaddles();
        assertNotNull(panel.paddle1, "Paddle1 should be created");
        assertNotNull(panel.paddle2, "Paddle2 should be created");
        assertEquals(0, panel.paddle1.x, "Paddle1 should be on left edge");
        assertEquals(GamePanel.GAME_WIDTH - GamePanel.PADDLE_WIDTH, panel.paddle2.x, 
                     "Paddle2 should be on right edge");
    }

    @Test
    void testDraw() {
        panel.draw(graphicsMock);
        // Each object in draw() should get a draw call
        // We can't precisely verify order or calls easily, but let's do some checks
        verify(graphicsMock, atLeastOnce()).fillRect(anyInt(), anyInt(), anyInt(), anyInt());
        verify(graphicsMock, atLeastOnce()).fillOval(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testCheckCollisionScoresAndResets() {
        // Force the ball to be out of left boundary
        panel.ball.x = -1;
        panel.checkCollision();
        assertEquals(1, panel.score.player2, "Player2 score should increment after ball leaves left boundary");
        
        // Force the ball to be out of right boundary
        panel.ball.x = GamePanel.GAME_WIDTH + 1;
        panel.checkCollision();
        assertEquals(1, panel.score.player1, "Player1 score should increment after ball leaves right boundary");
    }

    @Test
    void testCheckCollisionWithTopAndBottom() {
        // Test top boundary
        panel.ball.y = 0;
        panel.ball.setYDirection(-5);
        panel.checkCollision();
        assertTrue(panel.ball.yVelocity > 0, "Ball should bounce from top edge");
        
        // Test bottom boundary
        panel.ball.y = GamePanel.GAME_HEIGHT - GamePanel.BALL_DIAMETER;
        panel.ball.setYDirection(5);
        panel.checkCollision();
        assertTrue(panel.ball.yVelocity < 0, "Ball should bounce from bottom edge");
    }

    @Test
    void testKeyAdapter() {
        KeyEvent upEvent = mock(KeyEvent.class);
        when(upEvent.getKeyCode()).thenReturn(KeyEvent.VK_W);
        
        panel.newPaddles();
        panel.newBall();

        // Press W
        panel.new AL().keyPressed(upEvent);
        assertEquals(-panel.paddle1.speed, panel.paddle1.yVelocity, "Paddle1 velocity should change to -speed on W");
        // Release W
        panel.new AL().keyReleased(upEvent);
        assertEquals(0, panel.paddle1.yVelocity, "Paddle1 velocity should go back to 0 on release");
    }
}