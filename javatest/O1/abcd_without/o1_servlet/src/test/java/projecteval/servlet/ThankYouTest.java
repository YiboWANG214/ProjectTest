package projecteval.servlet;

import static org.mockito.Mockito.*;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/*
  Tests for thankyou.java. The servlet doPost has no logic except empty method. 
  We'll at least invoke it to ensure coverage. 
*/

@ExtendWith(MockitoExtension.class)
public class ThankYouTest {

    @Test
    public void testDoPost() throws ServletException, IOException {
        thankyou thankYouServlet = new thankyou();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        thankYouServlet.doPost(request, response);

        // Just verify that doPost doesn't throw or do anything unexpected
        verifyNoInteractions(request, response);
    }
}

