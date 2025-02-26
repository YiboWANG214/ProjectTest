package projecteval.bankingApplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class BankManagementTest {

    private Connection mockConnection;
    private Statement mockStatement;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private MockedStatic<connection> mockConnectionClass;

    @BeforeEach
    void setUp() throws Exception {
        // Mock the static method connection.getConnection()
        mockConnectionClass = Mockito.mockStatic(connection.class);
        mockConnection = Mockito.mock(Connection.class);
        mockStatement = Mockito.mock(Statement.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockResultSet = Mockito.mock(ResultSet.class);

        // By default, when connection.getConnection() is called, return our mockConnection
        mockConnectionClass.when(connection::getConnection).thenReturn(mockConnection);

        // Stubbing Behavior
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        mockConnectionClass.close(); // Close the mocked static
    }

    @Test
    void testCreateAccountSuccess() throws Exception {
        // Given
        when(mockStatement.executeUpdate(anyString())).thenReturn(1);

        // When
        boolean result = bankManagement.createAccount("JohnDoe", 1234);

        // Then
        assertTrue(result);
    }

    @Test
    void testCreateAccountEmptyName() throws Exception {
        // When
        boolean result = bankManagement.createAccount("", 1234);

        // Then
        assertFalse(result);
    }

    @Test
    void testCreateAccountZeroPasscode() throws Exception {
        // When
        boolean result = bankManagement.createAccount("ValidName", 0);

        // Then
        assertFalse(result);
    }

    @Test
    void testCreateAccountDuplicateName() throws Exception {
        // Simulate SQLIntegrityConstraintViolationException
        when(mockStatement.executeUpdate(anyString()))
                .thenThrow(new SQLIntegrityConstraintViolationException());

        boolean result = bankManagement.createAccount("DuplicateUser", 1234);
        assertFalse(result);
    }

    @Test
    void testLoginAccountSuccess() throws Exception {
        // Given
        when(mockResultSet.next()).thenReturn(true); // Simulate a valid user row
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Prepare to simulate user input for the post-login loop:
        // We'll provide: [ "5\n" ] quickly to exit the loop
        String simulatedInput = "5\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));

        // When
        boolean result = bankManagement.loginAccount("JohnDoe", 1234);

        // Then
        // If it found a matching record, it should return true
        assertTrue(result);
    }

    @Test
    void testLoginAccountEmptyFields() throws Exception {
        boolean result = bankManagement.loginAccount("", 1234);
        assertFalse(result);

        boolean result2 = bankManagement.loginAccount("Name", 0);
        assertFalse(result2);
    }

    @Test
    void testLoginAccountNotFound() throws Exception {
        // Suppose the query returns no rows
        when(mockResultSet.next()).thenReturn(false);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        boolean result = bankManagement.loginAccount("NonExistUser", 9999);
        assertFalse(result);
    }

    @Test
    void testGetBalance() throws Exception {
        // Given
        when(mockPreparedStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        // Suppose we have at least one row
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt("ac_no")).thenReturn(123);
        when(mockResultSet.getString("cname")).thenReturn("TestUser");
        when(mockResultSet.getInt("balance")).thenReturn(5000);

        // When / Then (no real assertion, just ensure it doesn't crash)
        bankManagement.getBalance(123);
    }

    @Test
    void testTransferMoneySuccess() throws Exception {
        // Given
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        // Suppose user has enough balance
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("balance")).thenReturn(2000);

        // Stubbing for Statement
        when(mockStatement.executeUpdate(anyString())).thenReturn(1);

        // When
        boolean result = bankManagement.transferMoney(1, 2, 500);

        // Then
        assertTrue(result);
    }

    @Test
    void testTransferMoneyInsufficientFunds() throws Exception {
        // Suppose user does not have enough balance
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("balance")).thenReturn(100); // less than amount
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        boolean result = bankManagement.transferMoney(1, 2, 500);
        assertFalse(result);
    }

    @Test
    void testTransferMoneyInvalidFields() throws Exception {
        // The method checks for "reveiver_ac == NULL or amount == NULL"
        // passing 0 for these arguments will cause it to fail
        boolean result = bankManagement.transferMoney(1, 0, 0);
        assertFalse(result);
    }

    @Test
    void testTransferMoneySQLException() throws Exception {
        // Force an exception
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("balance")).thenReturn(2000);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("SQL Error!"));

        boolean result = bankManagement.transferMoney(1, 2, 500);
        // We expect rollback => method returns false
        assertFalse(result);
    }
}

//--------------------------------------------------------------
// ConnectionTest.java
//--------------------------------------------------------------
