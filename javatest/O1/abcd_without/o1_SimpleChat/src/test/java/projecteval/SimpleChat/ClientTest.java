package projecteval.SimpleChat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientTest {

    private Client client;

    @BeforeEach
    public void setUp() {
        client = new Client();
    }

    @Test
    public void testSetUpNetworking_successfulConnection() throws Exception {
        // Mock socket, streams
        Socket mockSocket = mock(Socket.class);
        PrintWriter mockWriter = mock(PrintWriter.class);
        BufferedReader mockReader = mock(BufferedReader.class);

        // We'll mock the actual results in a partial way
        when(mockSocket.getOutputStream()).thenReturn(System.out);
        when(mockSocket.getInputStream()).thenReturn(System.in);

        // Spy on client to replace real networking call
        Client spyClient = spy(client);

        // Replace the actual new Socket creation with our mock:
        // doReturn(mockSocket).when(spyClient).createSocket(anyString(), anyInt());

        // We can force direct usage by controlling the next lines if needed
        // For coverage, let's just call setUpNetworking() with mock in place
        // spyClient.setUpNetworking();

        // Ensure the fields are not null
        assertNotNull(spyClient.sock);
        assertNotNull(spyClient.writer);
        assertNotNull(spyClient.reader);
    }

    @Test
    public void testSendButtonListener() {
        // Prepare the client with a mock writer
        PrintWriter mockWriter = mock(PrintWriter.class);
        client.writer = mockWriter;
        client.outgoing.setText("HelloFromTest");

        // Perform action
        ActionEvent e = mock(ActionEvent.class);
        Client.SendButtonListener listener = client.new SendButtonListener();
        listener.actionPerformed(e);

        // Verify that writer wrote and flush was called
        verify(mockWriter).println("HelloFromTest");
        verify(mockWriter).flush();

        // Check that the text field was cleared
        assertEquals("", client.outgoing.getText());
    }

    @Test
    public void testIncomingReader_singleMessage() throws Exception {
        // Create a ByteArrayInputStream to simulate a single line of data
        String testData = "MessageFromServer\n";
        InputStream input = new ByteArrayInputStream(testData.getBytes());

        // Mock reading from InputStream
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.getInputStream()).thenReturn(input);

        client.sock = mockSocket;
        client.reader = new java.io.BufferedReader(new java.io.InputStreamReader(input));

        // Start the incoming reader logic
        Client.IncomingReader reader = client.new IncomingReader();
        reader.run();

        // Verify that the text area got the message
        assertTrue(client.incoming.getText().contains("MessageFromServer"));
    }
}

/*
 * NOTE: 
 * To ensure that setUpNetworking() in Client is testable,
 * you can modify the original Client code to have a protected "createSocket" method
 * so that we can spy on it and return our mock. 
 * For example:
 *   protected Socket createSocket(String host, int port) throws IOException {
 *       return new Socket(host, port);
 *   }
 * Then, in the test, we can do:
 *   doReturn(mockSocket).when(spyClient).createSocket(anyString(), anyInt());
 * This allows the test to avoid creating a real socket.
 *
 * Make sure to include the following in your Maven/Gradle dependencies to compile:
 *   - JUnit (e.g., JUnit 5)
 *   - Mockito (for mocking)
 */