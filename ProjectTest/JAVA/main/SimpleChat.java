// SimpleChat/Server.java

package projecteval.SimpleChat;

import java.io.*;
import java.net.*;
import java.util.*;

class VerySimpleChatServer {
  ArrayList clientOutputStreams;

  public class ClientHandler implements Runnable {
    BufferedReader reader;
    Socket sock;

    public ClientHandler(Socket clientSocket) {
      try {
        sock = clientSocket;
        InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
        reader = new BufferedReader(isReader);

      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } // close constructor

    public void run() {
      String message;
      try {
        while ((message = reader.readLine()) != null) {
          System.out.println("read " + message);
          tellEveryone(message);

        } // close while
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } // close run
  }

  // close inner class
  public static void main(String[] args) {
    new VerySimpleChatServer().go();
  }

  public void go() {
    clientOutputStreams = new ArrayList();
    try {
      ServerSocket serverSock = new ServerSocket(5000);
      while (true) {
        Socket clientSocket = serverSock.accept();
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        clientOutputStreams.add(writer);
        Thread t = new Thread(new ClientHandler(clientSocket));
        t.start();
        System.out.println("got a connection");
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  } // close go

  public void tellEveryone(String message) {
    Iterator it = clientOutputStreams.iterator();
    while (it.hasNext()) {
      try {
        PrintWriter writer = (PrintWriter) it.next();
        writer.println(message);
        writer.flush();
      } catch (Exception ex) {
        ex.printStackTrace();
      }

    } // end while

  } // close tellEveryone
} // close class

// SimpleChat/Client.java

package projecteval.SimpleChat;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Client {
  JTextArea incoming;
  JTextField outgoing;
  BufferedReader reader;
  PrintWriter writer;
  Socket sock;

  public static void main(String[] args) {
    Client client = new Client();
    client.go();
  }

  public void go() {
    JFrame frame = new JFrame("Ludicrously Simple Chat Client");
    JPanel mainPanel = new JPanel();

    incoming = new JTextArea(15, 50);

    incoming.setLineWrap(true);
    incoming.setWrapStyleWord(true);
    incoming.setEditable(false);

    JScrollPane qScroller = new JScrollPane(incoming);
    qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    outgoing = new JTextField(20);
    JButton sendButton = new JButton("Send");
    sendButton.addActionListener(new SendButtonListener());

    mainPanel.add(qScroller);
    mainPanel.add(outgoing);
    mainPanel.add(sendButton);
    
    setUpNetworking();

    Thread readerThread = new Thread(new IncomingReader());
    
    readerThread.start();

    frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
    frame.setSize(400, 500);
    frame.setVisible(true);
  }

  private void setUpNetworking() {
    try {
      sock = new Socket("192.168.5.16", 5000);
      InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
      reader = new BufferedReader(streamReader);
      writer = new PrintWriter(sock.getOutputStream());
      System.out.println("networking established");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  } // close setUpNetworking

  public class SendButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent ev) {
      try {
        writer.println(outgoing.getText());
        writer.flush();

      } catch (Exception ex) {
        ex.printStackTrace();
      }
      outgoing.setText("");
      outgoing.requestFocus();
    }
  } // close inner class

  public class IncomingReader implements Runnable {
    public void run() {
      String message;
      try {
        while ((message = reader.readLine()) != null) {
          System.out.println("read " + message);
          incoming.append(message + "\n");

        } // close while
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } // close run
  }
}

