package projecteval.QuizUp.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.QuizUp.Login;
import projecteval.QuizUp.Quiz;
import projecteval.QuizUp.Rules;
import projecteval.QuizUp.Score;

/* ---------------------------------------------------------------------------------
 * ScoreTest
 * ---------------------------------------------------------------------------------
 */
@ExtendWith(MockitoExtension.class)
class ScoreTest {

    @Test
    @DisplayName("Test Score Frame Initialization")
    void testScoreInitialization() {
        // Instantiating Score
        Score scoreWindow = new Score("MockUser", 50);
        
        // Basic verifications 
        assertEquals("MockUser", ((javax.swing.JLabel)scoreWindow.getContentPane().getComponent(1)).getText().substring(9, 17),
                "The heading should contain the username");
        assertTrue(scoreWindow.isVisible(), "Score window should be visible");
    }

    @Test
    @DisplayName("Test Score ActionListener (Play Again)")
    void testScoreActionListener() throws NoSuchMethodException, SecurityException, IllegalAccessException, 
                                          IllegalArgumentException, InvocationTargetException {
        Score scoreWindow = new Score("MockUser", 100);

        // Find the "Play Again" button
        // We know the button is added last among the components in the constructor:
        JButton playAgainButton = null;
        for (int i = 0; i < scoreWindow.getContentPane().getComponentCount(); i++) {
            if (scoreWindow.getContentPane().getComponent(i) instanceof JButton) {
                JButton temp = (JButton) scoreWindow.getContentPane().getComponent(i);
                if ("Play Again".equals(temp.getText())) {
                    playAgainButton = temp;
                    break;
                }
            }
        }
        assertNotNull(playAgainButton, "Play Again button should be present");

        // Simulate button click
        ActionListener[] listeners = playAgainButton.getActionListeners();
        for (ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(playAgainButton, ActionEvent.ACTION_PERFORMED, "Play Again"));
        }

        // After actionPerformed, Score window should no longer be visible
        assertFalse(scoreWindow.isVisible(), "Score window should be hidden after clicking Play Again");
    }
}

/* ---------------------------------------------------------------------------------
 * QuizTest
 * ---------------------------------------------------------------------------------
 */
@ExtendWith(MockitoExtension.class)
class QuizTest {
    
    @Mock
    Connection mockConnection;
    
    @Mock
    PreparedStatement mockPreparedStmt;
    
    @Mock
    ResultSet mockResultSetQuestions;
    
    @Mock
    ResultSet mockResultSetOptions;
    
    @Mock
    ResultSet mockResultSetSolutions;
    
    @InjectMocks
    Quiz quizUnderTest;  // We will manually inject mocks in @BeforeEach via reflection or reconstruction
    
    @BeforeEach
    void setUp() throws SQLException {
        // We will construct Quiz with a known user name
        quizUnderTest = new Quiz("TestUser");
        // Because the constructor tries to open a DB connection, 
        // we need to forcibly replace that connection with our mockConnection.
        quizUnderTest.connection = mockConnection;
    }
    
    @Test
    @DisplayName("Test retrieveDataFromDatabase with partial mock results")
    void testRetrieveDataFromDatabase() throws SQLException {
        // Setup mock for questions
        when(mockConnection.prepareStatement("SELECT * FROM quiz_questions")).thenReturn(mockPreparedStmt);
        when(mockPreparedStmt.executeQuery()).thenReturn(mockResultSetQuestions);

        // We will mock 2 questions for demonstration
        when(mockResultSetQuestions.next()).thenReturn(true, true, false);
        when(mockResultSetQuestions.getInt("question_id")).thenReturn(101, 102);
        when(mockResultSetQuestions.getString("question_text"))
            .thenReturn("Q1 question text?", "Q2 question text?");

        // Mock for retrieving choices
        PreparedStatement mockChoiceStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(startsWith("SELECT * FROM quiz_choices"))).thenReturn(mockChoiceStatement);
        when(mockChoiceStatement.executeQuery()).thenReturn(mockResultSetOptions);
        
        when(mockResultSetOptions.next()).thenReturn(true, true, true, true, false, true, true, true, true, false);
        // 2 sets of 4 choices for each question => total 8 times "true", plus extra times for demonstration

        // Return strings for each next() iteration
        // We'll only do some sample answer text
        when(mockResultSetOptions.getString("choice_text"))
            .thenReturn("A1", "B1", "C1", "D1", 
                        "A2", "B2", "C2", "D2");

        // Mock for solutions
        PreparedStatement mockSolutionStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(startsWith("SELECT * FROM quiz_answers"))).thenReturn(mockSolutionStatement);
        when(mockSolutionStatement.executeQuery()).thenReturn(mockResultSetSolutions);
        
        when(mockResultSetSolutions.next()).thenReturn(true, false, true, false);
        when(mockResultSetSolutions.getString("answer_text"))
            .thenReturn("A1", "A2");

        // Call method under test
        quizUnderTest.retrieveDataFromDatabase();

        // Verify that the statements are prepared at least once
        verify(mockConnection, atLeastOnce()).prepareStatement("SELECT * FROM quiz_questions");
        verify(mockPreparedStmt, atLeastOnce()).executeQuery();
        
        // There's no direct assertion for the arrays being properly set except verifying no crash
        // but we can check that the arrays got some data
        assertEquals("Q1 question text?", quizUnderTest.questions[0][0]);
        assertEquals("A1", quizUnderTest.questions[0][1]);
        assertEquals("A1", quizUnderTest.answers[0][1]);
    }

    @Test
    @DisplayName("Test actionPerformed: Next Button with selection")
    void testQuizActionPerformedNext() {
        // Simulate that we have some question/answers in the arrays:
        quizUnderTest.questions[0][0] = "Q1?";
        quizUnderTest.questions[0][1] = "A";
        quizUnderTest.questions[0][2] = "B";
        quizUnderTest.questions[0][3] = "C";
        quizUnderTest.questions[0][4] = "D";
        
        quizUnderTest.answers[0][1] = "A";
        
        // Ensure the radio buttons reflect these
        quizUnderTest.start(0);

        // Force a selection (simulate user choosing the first option)
        quizUnderTest.opt1.setSelected(true);

        // Create an action event for the "Next" button
        ActionEvent event = new ActionEvent(quizUnderTest.next, ActionEvent.ACTION_PERFORMED, "Next");
        quizUnderTest.actionPerformed(event);
        
        // The useranswers for question 0 should be "A"
        assertEquals("A", quizUnderTest.useranswers[0][0]);
        
        // The count should have incremented
        assertEquals(1, Quiz.count, "After clicking Next, the question index 'count' should be 1");
    }

    @Test
    @DisplayName("Test actionPerformed: Submit Button correctness scoring")
    void testQuizActionPerformedSubmit() {
        // Fill arrays with a known scenario
        quizUnderTest.questions[0][0] = "Q1?";
        quizUnderTest.questions[0][1] = "A";
        quizUnderTest.questions[0][2] = "B";
        quizUnderTest.questions[0][3] = "C";
        quizUnderTest.questions[0][4] = "D";
        quizUnderTest.answers[0][1] = "A";
        
        quizUnderTest.start(0);
        quizUnderTest.opt1.setSelected(true);

        // Move count to the last question to simulate near-end quiz
        Quiz.count = 0;

        // Press submit
        ActionEvent event = new ActionEvent(quizUnderTest.submit, ActionEvent.ACTION_PERFORMED, "Submit");
        quizUnderTest.actionPerformed(event);

        // Score should reflect correct answer: +10 points
        assertEquals(10, Quiz.score, "Score should be 10 after correct answer submit");
    }

    @Test
    @DisplayName("Test paint() Timer Decrements")
    void testQuizPaintTimer() {
        // We can't easily test the entire paint cycle, but we can check that timer is set to 15 initially
        assertEquals(15, Quiz.timer, "Initial timer must be 15");
        // We can force a small repaint call:
        quizUnderTest.repaint();
        // The code decrements the timer in the paint method, but that also sleeps 1 second in a loop.
        // We won't test the actual UI painting here. Just a sanity check that the variable is accessible.
        // No assertion needed besides no exceptions thrown.
    }
}

/* ---------------------------------------------------------------------------------
 * LoginTest
 * ---------------------------------------------------------------------------------
 */
@ExtendWith(MockitoExtension.class)
class LoginTest {
    
    @InjectMocks
    Login loginUnderTest;
    
    @BeforeEach
    void setUp() {
        loginUnderTest = new Login();
    }
    
    @Test
    @DisplayName("Test Login GUI Initialization")
    void testLoginInitialization() {
        assertTrue(loginUnderTest.isVisible(), "Login window should be visible initially");
    }
    
    @Test
    @DisplayName("Test Proceed Button Action")
    void testLoginProceedAction() {
        // Find text field and set name
        JTextField tf = null;
        for (int i = 0; i < loginUnderTest.getContentPane().getComponentCount(); i++) {
            if (loginUnderTest.getContentPane().getComponent(i) instanceof JTextField) {
                tf = (JTextField) loginUnderTest.getContentPane().getComponent(i);
                break;
            }
        }
        assertNotNull(tf, "Should have a text field for name input");
        tf.setText("JohnDoe");
        
        // Find Proceed Button
        JButton proceedBtn = null;
        for (int i = 0; i < loginUnderTest.getContentPane().getComponentCount(); i++) {
            if (loginUnderTest.getContentPane().getComponent(i) instanceof JButton) {
                JButton temp = (JButton) loginUnderTest.getContentPane().getComponent(i);
                if ("Proceed".equals(temp.getText())) {
                    proceedBtn = temp;
                    break;
                }
            }
        }
        assertNotNull(proceedBtn, "Should have a Proceed button");
        
        // Simulate button click
        ActionListener[] listeners = proceedBtn.getActionListeners();
        for (ActionListener l : listeners) {
            l.actionPerformed(new ActionEvent(proceedBtn, ActionEvent.ACTION_PERFORMED, "Proceed"));
        }
        
        // The Login window should be hidden
        assertFalse(loginUnderTest.isVisible(), "Login window should be hidden after clicking Proceed");
    }
    
    @Test
    @DisplayName("Test Back Button Action")
    void testBackButtonAction() {
        // Find Back Button
        JButton backBtn = null;
        for (int i = 0; i < loginUnderTest.getContentPane().getComponentCount(); i++) {
            if (loginUnderTest.getContentPane().getComponent(i) instanceof JButton) {
                JButton temp = (JButton) loginUnderTest.getContentPane().getComponent(i);
                if ("Back".equals(temp.getText())) {
                    backBtn = temp;
                    break;
                }
            }
        }
        assertNotNull(backBtn, "Should have a Back button");
        
        // Simulate button click
        ActionListener[] listeners = backBtn.getActionListeners();
        for (ActionListener l : listeners) {
            l.actionPerformed(new ActionEvent(backBtn, ActionEvent.ACTION_PERFORMED, "Back"));
        }
        
        // The Login window should be hidden
        assertFalse(loginUnderTest.isVisible(), "Login window should be hidden after clicking Back");
    }
}

/* ---------------------------------------------------------------------------------
 * RulesTest
 * ---------------------------------------------------------------------------------
 */
@ExtendWith(MockitoExtension.class)
class RulesTest {
    
    @InjectMocks
    Rules rulesUnderTest;  // We will reconstruct in @BeforeEach
    
    @BeforeEach
    void setUp() {
        rulesUnderTest = new Rules("TestUser");
    }
    
    @Test
    @DisplayName("Test Rules Window Initialization")
    void testRulesInitialization() {
        assertTrue(rulesUnderTest.isVisible(), "Rules window should be visible initially");
    }
    
    @Test
    @DisplayName("Test Start Button Action")
    void testStartButtonAction() {
        JButton startButton = null;
        for (int i = 0; i < rulesUnderTest.getContentPane().getComponentCount(); i++) {
            if (rulesUnderTest.getContentPane().getComponent(i) instanceof JButton) {
                JButton temp = (JButton) rulesUnderTest.getContentPane().getComponent(i);
                if ("Start".equals(temp.getText())) {
                    startButton = temp;
                    break;
                }
            }
        }
        assertNotNull(startButton, "Should have a Start button");
        
        ActionListener[] listeners = startButton.getActionListeners();
        for (ActionListener l : listeners) {
            l.actionPerformed(new ActionEvent(startButton, ActionEvent.ACTION_PERFORMED, "Start"));
        }
        
        // The Rules window should be hidden
        assertFalse(rulesUnderTest.isVisible(), "Rules window should be hidden after clicking Start");
    }

    @Test
    @DisplayName("Test Back Button Action")
    void testBackButtonAction() {
        JButton backButton = null;
        for (int i = 0; i < rulesUnderTest.getContentPane().getComponentCount(); i++) {
            if (rulesUnderTest.getContentPane().getComponent(i) instanceof JButton) {
                JButton temp = (JButton) rulesUnderTest.getContentPane().getComponent(i);
                if ("Back".equals(temp.getText())) {
                    backButton = temp;
                    break;
                }
            }
        }
        assertNotNull(backButton, "Should have a Back button");
        
        ActionListener[] listeners = backButton.getActionListeners();
        for (ActionListener l : listeners) {
            l.actionPerformed(new ActionEvent(backButton, ActionEvent.ACTION_PERFORMED, "Back"));
        }
        
        // The Rules window should be hidden
        assertFalse(rulesUnderTest.isVisible(), "Rules window should be hidden after clicking Back");
    }
}

/* 
-----------------------------------------------------------------------------------------
Compile & Run Notes:
-----------------------------------------------------------------------------------------
1. These tests require JUnit 5 (i.e., the JUnit Jupiter API) and Mockito to be on the classpath.
2. The test classes assume your main source code is in package "projecteval.QuizUp".
3. The tests employ a standard Maven/Gradle style folder structure:
   - Source in: src/main/java/projecteval/QuizUp/*.java
   - Tests in:  src/test/java/projecteval/QuizUp/tests/*.java
4. Make sure your test framework is configured accordingly.
5. Since these classes open Swing GUIs, you may want to run tests in headless mode or ensure
   the tests do not block. The tests are fairly simple checks for coverage and compilation.
*/