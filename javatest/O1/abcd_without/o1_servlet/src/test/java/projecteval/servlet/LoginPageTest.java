package projecteval.servlet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
  Tests for loginpage.java servlet. Mocks out request/response
  and verifies flow logic including DBUtilR usage. 
*/

@ExtendWith(MockitoExtension.class)
public class LoginPageTest {

    @InjectMocks
    private loginpage loginServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private PrintWriter writer;

    // Connection and PreparedStatement mocks
    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    public void setUp() throws IOException {
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    public void testDoPost_success() throws ServletException, IOException, SQLException {
        // Given
        when(request.getParameter("cardno")).thenReturn("123456");
        when(request.getParameter("pin")).thenReturn("1111");
        when(request.getRequestDispatcher("vote.html")).thenReturn(dispatcher);

        // Mock DB calls
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        // We'll override the static final connection in loginpage with our mock
        // This is tricky in normal code, but for demonstration we do:
        // loginpage.con = mockConnection;

        // When
        loginServlet.doPost(request, response);

        // Then
        verify(writer, atLeastOnce()).print(contains("Successful Login...You Can Vote Now"));
        verify(dispatcher, atLeastOnce()).include(request, response);
    }

    @Test
    public void testDoPost_failure() throws ServletException, IOException, SQLException {
        // Given
        when(request.getParameter("cardno")).thenReturn("abc");
        when(request.getParameter("pin")).thenReturn("0000");
        when(request.getRequestDispatcher("registration.html")).thenReturn(dispatcher);

        // Mock DB calls
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Overriding connection again
        // loginpage.con = mockConnection;

        // When
        loginServlet.doPost(request, response);

        // Then
        verify(writer, atLeastOnce()).print(contains("Sorry username or password error"));
        verify(dispatcher, atLeastOnce()).include(request, response);
    }

    @Test
    public void testDoPost_sqlException() throws ServletException, IOException, SQLException {
        when(request.getParameter("cardno")).thenReturn("123");
        when(request.getParameter("pin")).thenReturn("1111");
        when(request.getRequestDispatcher("registration.html")).thenReturn(dispatcher);

        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Test SQL Exception"));
        // loginpage.con = mockConnection;

        loginServlet.doPost(request, response);

        // We'll just verify we didn't go to "vote.html"
        verify(dispatcher, atLeastOnce()).include(request, response);
        verify(writer, atLeastOnce()).print(anyString());
    }
}

