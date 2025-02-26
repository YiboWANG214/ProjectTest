package projecteval.servlet;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
  These tests illustrate how to unit-test a servlet class using JUnit and Mockito.
  The database connections in the production code are direct (i.e., Class.forName, etc.),
  so here we primarily show how to mock the request/response and control flow logic.
  In real scenarios, you'd refactor the DB logic into a DAO or use advanced mocking tools
  for the DriverManager.
*/


@ExtendWith(MockitoExtension.class)
public class RegistrationTest {
    
    @InjectMocks
    private registration registrationServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private PrintWriter writer;

    @BeforeEach
    public void setUp() throws IOException {
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    public void testDoPost_success() throws ServletException, IOException {
        // Mock request parameters
        when(request.getParameter("fname")).thenReturn("John");
        when(request.getParameter("cardno")).thenReturn("123456");
        when(request.getParameter("cono")).thenReturn("9876543210");
        when(request.getParameter("add")).thenReturn("Some Address");
        when(request.getParameter("dob")).thenReturn("2000-01-01");
        when(request.getParameter("email")).thenReturn("john@example.com");
        when(request.getParameter("pin")).thenReturn("1111");
        
        // Mock dispatcher
        when(request.getRequestDispatcher("loginpage.html")).thenReturn(dispatcher);

        // Execute
        registrationServlet.doPost(request, response);

        // Verify expected flow
        verify(writer, atLeastOnce()).print(contains("Successfully your account has been created"));
        verify(dispatcher, atLeastOnce()).include(request, response);
    }

    @Test
    public void testDoPost_exception() throws ServletException, IOException {
        // Simulate missing parameter or some error
        when(request.getParameter("fname")).thenReturn(null);

        // Mock dispatcher
        when(request.getRequestDispatcher("registration.html")).thenReturn(dispatcher);

        // Execute
        registrationServlet.doPost(request, response);

        // Verify error message
        verify(writer, atLeastOnce()).print(contains("Invalid , Failed account creation try again"));
        verify(dispatcher, atLeastOnce()).include(request, response);
    }

    @Test
    public void testServiceMethod() throws ServletException, IOException {
        // Just ensure doPost is called from service
        registrationServlet.service(request, response);
        verify(request, times(1)).getParameter(anyString());
    }
}

