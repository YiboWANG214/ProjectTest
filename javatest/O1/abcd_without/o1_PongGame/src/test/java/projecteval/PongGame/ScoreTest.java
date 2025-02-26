package projecteval.PongGame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;


public class ScoreTest {
    
    private Score score;
    private Graphics graphicsMock;
    private final int WIDTH = 800;
    private final int HEIGHT = 600;

    @BeforeEach
    void setup() {
        score = new Score(WIDTH, HEIGHT);
        graphicsMock = mock(Graphics.class);
    }

    @Test
    void testScoreInitialization() {
        assertEquals(WIDTH, Score.GAME_WIDTH, "Game width should match assigned value");
        assertEquals(HEIGHT, Score.GAME_HEIGHT, "Game height should match assigned value");
        assertEquals(0, score.player1, "Player 1 score should start at 0");
        assertEquals(0, score.player2, "Player 2 score should start at 0");
    }

    @Test
    void testScoreDrawsCorrectly() {
        // Set some scores
        score.player1 = 12;
        score.player2 = 7;

        // Call draw
        score.draw(graphicsMock);

        // Verify calls to Graphics
        verify(graphicsMock).setColor(Color.white);
        verify(graphicsMock).setFont(new Font("Consolas", Font.PLAIN, 60));
        verify(graphicsMock).drawLine(score.GAME_WIDTH / 2, 0, score.GAME_WIDTH / 2, score.GAME_HEIGHT);
        verify(graphicsMock).drawString("12", (score.GAME_WIDTH / 2) - 85, 50);
        verify(graphicsMock).drawString("07", (score.GAME_WIDTH / 2) + 20, 50);
    }
}

/******************************************************************************
 * GameFrameTest.java
 ******************************************************************************/
