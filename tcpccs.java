import java.io.*;
import java.net.*;

class tcpccs {
    private static final int PORT = 12345;
    public static volatile boolean running = true;
    public static void main(String[] args) throws Exception {

        // Register a shutdown hook to catch Ctrl+C (SIGINT)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCtrl+C detected. Stopping the main thread...");
            running = false; // This will break the main loop
        }));

        if (args.length != 2) {
            System.out.println("Usage: java tcpccs <hostname> <username>");
            System.exit(1);
        }

        String hostname = args[0];
        String username = args[1];

        // establishes connection to server
        Socket clientSocket = new Socket(hostname, PORT);
        System.out.println("Connected to the server. You can start sending messages.");

        // Communication Streams
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Thread for listening for server messages
        // Passes the clients connection and the server message
        Thread serverMessageListenerThread = new Thread(new ServerMessageListener(clientSocket, inFromServer));
        serverMessageListenerThread.start();

        // Thread for sending messages to the server
        Thread inputThread = new Thread(new clientMessageInput(clientSocket, inFromUser, outToServer, username));
        inputThread.start();
    }   

}

class ServerMessageListener implements Runnable {
    private Socket clientSocket;
    private BufferedReader inFromServer;

    public ServerMessageListener(Socket socket, BufferedReader inFromServer) {
        this.clientSocket = socket;
        this.inFromServer = inFromServer;
    }

    @Override
    public void run() {
        try {
            String serverMessage;
            // Continuously listen for messages from the server
            while ((serverMessage = inFromServer.readLine()) != null) {
                System.out.println(serverMessage);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}

class clientMessageInput implements Runnable {
    private Socket clientSocket;
    private BufferedReader inFromUser;
    private DataOutputStream outToServer;
    private String username;

    public clientMessageInput(Socket socket, BufferedReader inFromUser, DataOutputStream outToServer, String username) {
        this.clientSocket = socket;
        this.inFromUser = inFromUser;
        this.outToServer = outToServer;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            String clientMessage;
            while (tcpccs.running) {
                clientMessage = inFromUser.readLine();

                if (clientMessage.equals("/quit")) {
                    break;
                }
                String formattedMessage = username + ": " + clientMessage;
                outToServer.writeBytes(formattedMessage + "\n");
            }
        } catch (IOException e) { 
            System.out.println(e);
        } finally {
            try {
                // Close the socket and streams when quitting
                outToServer.close();
                clientSocket.close();
                inFromUser.close();
                System.out.println("Disconnected from the server.");
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}