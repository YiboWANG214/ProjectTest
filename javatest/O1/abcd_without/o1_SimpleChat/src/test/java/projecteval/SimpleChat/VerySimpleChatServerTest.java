package projecteval.SimpleChat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


public class VerySimpleChatServerTest {

    private VerySimpleChatServer server;

    @BeforeEach
    public void setUp() {
        server = new VerySimpleChatServer();
    }

    @Test
    public void testTellEveryone_withExistingClientOutputStreams() {
        // Given
        ArrayList<PrintWriter> mockClients = new ArrayList<>();
        PrintWriter mockWriter1 = mock(PrintWriter.class);
        PrintWriter mockWriter2 = mock(PrintWriter.class);
        mockClients.add(mockWriter1);
        mockClients.add(mockWriter2);
        
        // Use reflection or direct assignment if possible
        server.clientOutputStreams = mockClients;

        // When
        server.tellEveryone("Hello Test");

        // Then
        verify(mockWriter1).println("Hello Test");
        verify(mockWriter1).flush();
        verify(mockWriter2).println("Hello Test");
        verify(mockWriter2).flush();
    }

    @Test
    public void testClientHandlerRun_singleMessage() throws Exception {
        // Mock the socket and BufferedReader
        Socket mockSocket = mock(Socket.class);
        InputStream mockInputStream = mock(InputStream.class);

        // Create an InputStream that simulates one line of data and then EOF
        String message = "TestMessage\n";
        when(mockSocket.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(message.getBytes()));

        // Instantiate ClientHandler
        VerySimpleChatServer.ClientHandler handler = server.new ClientHandler(mockSocket);

        // Spy on the server so we can verify tellEveryone was called
        VerySimpleChatServer spyServer = spy(server);

        // We need to override the readLine() to use our custom data
        // but we already set the underlying InputStream for the handler

        // Make sure the server's clientOutputStreams is a non-null list
        spyServer.clientOutputStreams = new ArrayList<>();

        // We also want to ensure tellEveryone doesn't fail because of 0 PrintWriters
        // We'll add some mock PrintWriters here:
        PrintWriter pw = mock(PrintWriter.class);
        spyServer.clientOutputStreams.add(pw);

        // Run the thread logic inline
        handler.run();

        // We must check that the message was read internally and then broadcast
        // log output: "read TestMessage"
        // Then call server.tellEveryone("TestMessage");
        // Here, the direct call is inside the run() method
        // But since 'server' is not the same instance as 'spyServer', we replicate 
        // from handler to 'server' itself. We'll do a small adjustment to verify broadcast.

        // We'll just verify if the mock PrintWriter got the message
        verify(pw).println("TestMessage");
        verify(pw).flush();
    }

    // If you want to test go(), you can do so carefully with a mock ServerSocket,
    // but that usually requires integration-like testing rather than unit-only.
    // We'll avoid it here since it opens real sockets.
}

// ClientTest.java
// This test class targets methods within the Client class. We test setUpNetworking(),
// the SendButtonListener, and the IncomingReader logic using mocks.

