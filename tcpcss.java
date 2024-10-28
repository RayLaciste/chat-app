import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

class tcpcss {
    public static volatile boolean running = true;
    private static final int PORT = 12345;
    private static final ConcurrentHashMap<String, Client> clientList = new ConcurrentHashMap<>();
    private static int numClient = 0;
    public static void main(String[] args) throws Exception {

        // Register a shutdown hook to catch Ctrl+C (SIGINT)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCtrl+C detected. Stopping the main thread...");
            running = false; // This will break the main loop
        }));

        // Listens for incoming client connections
        ServerSocket welcomeSocket = new ServerSocket(PORT);
        System.out.println("Listening on port " + PORT);
        System.out.println("Waiting for connections...");

        while (running) {
            // When a client connects, this represents the connection for that client
            // New socket created for each client
            Socket connectionSocket = welcomeSocket.accept();

            // Create a new client object -> attach the socket to the client object
            Client client = new Client(connectionSocket, clientList);
            clientList.put(connectionSocket.toString(), client);

            Thread clientThread = new Thread(client);
            //invokes run
            clientThread.start();

            int socketCount = numClient++;
            System.out.println("New connection, thread name is " + clientThread.getName() +
                                ", ip is: " + client.getAddress() + 
                                ", port is: " + client.getPort());
            System.out.println("Adding to list of sockets as " + socketCount);
        }  
         
        welcomeSocket.close();
    }
}

class Client implements Runnable {
    private Socket clientSocket;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    private ConcurrentHashMap<String, Client> clientList;

    public Client (Socket socket, ConcurrentHashMap<String, Client> clientList) {
        this.clientSocket = socket;
        this.clientList = clientList;
    }

    @Override
    public void run() {
        try {
            inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToClient = new DataOutputStream(clientSocket.getOutputStream());
            
            String clientMessage;
            while (tcpcss.running) {
                clientMessage = inFromClient.readLine();

                // if client disconnects (it becomes null)
                if (clientMessage == null) {
                    break;
                }
                System.out.println("client: " + clientMessage);
                sendToAll(clientMessage);
            }
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            disconnectCleanup();
        }
    }

    public void sendToAll(String message) {
        // for each client in the list, if the values of the client...
        for (Client client : clientList.values()) {
            if (client != this) { // is not the same - send to the other clients
                try {
                    client.outToClient.writeBytes(message + "\n");
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
    }

    private void disconnectCleanup() {
        try {
            clientSocket.close();
            System.out.println("Host disconnected, ip " + getAddress() +
                                ", port " + getPort());
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            clientList.remove(clientSocket.toString());
        }
    }

    public String getAddress() {
        return clientSocket.getInetAddress().getHostAddress();
    }

    public int getPort() {
        return clientSocket.getPort();
    }
}