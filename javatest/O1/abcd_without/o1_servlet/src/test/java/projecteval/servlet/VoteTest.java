package projecteval.servlet;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

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
  Tests for vote.java. Mocks request/response and ensures coverage on doPost.
*/

@ExtendWith(MockitoExtension.class)
public class VoteTest {

    @InjectMocks
    private vote voteServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private PrintWriter writer;

    @Mock
    private RequestDispatcher dispatcher;

    // DB objects
    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStatement;

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
        when(request.getParameter("party")).thenReturn("PartyX");
        when(request.getRequestDispatcher("thankyou.html")).thenReturn(dispatcher);

        // DB logic
        when(mockConnection.prepareStatement(startsWith("Select * from register"))).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        when(mockConnection.prepareStatement(startsWith("insert into vote"))).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        // Override final static con in vote with mock
        // vote.con = mockConnection;

        // When
        voteServlet.doPost(request, response);

        // Then
        verify(writer, atLeastOnce()).print(contains("Your Vote has been submitted successfully"));
        verify(dispatcher, atLeastOnce()).include(request, response);
    }

    @Test
    public void testDoPost_invalidCardNo() throws ServletException, IOException, SQLException {
        // Given
        when(request.getParameter("cardno")).thenReturn("abc");
        when(request.getParameter("party")).thenReturn("PartyX");
        when(request.getRequestDispatcher("vote.html")).thenReturn(dispatcher);

        // DB logic to simulate no match
        when(mockConnection.prepareStatement(startsWith("Select * from register"))).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // vote.con = mockConnection;

        // When
        voteServlet.doPost(request, response);

        // Then
        verify(writer, atLeastOnce()).print(contains("Please enter correct card number"));
        verify(dispatcher, atLeastOnce()).include(request, response);
    }

    @Test
    public void testDoPost_SQLIntegrityConstraintViolationException() throws SQLException, ServletException, IOException {
        // Given
        when(request.getParameter("cardno")).thenReturn("123456");
        when(request.getParameter("party")).thenReturn(null);
        when(request.getRequestDispatcher("vote.html")).thenReturn(dispatcher);

        when(mockConnection.prepareStatement(startsWith("Select * from register"))).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        when(mockConnection.prepareStatement(startsWith("insert into vote"))).thenThrow(new SQLIntegrityConstraintViolationException());
        // vote.con = mockConnection;

        // When
        voteServlet.doPost(request, response);

        // Then
        verify(writer, atLeastOnce()).print(contains("Please select any party"));
        verify(dispatcher, atLeastOnce()).include(request, response);
    }

    @Test
    public void testServiceCallsDoPost() throws ServletException, IOException {
        voteServlet.service(request, response);
        // doPost is called inside service
        verify(request, times(1)).getParameter(anyString());
    }
}
