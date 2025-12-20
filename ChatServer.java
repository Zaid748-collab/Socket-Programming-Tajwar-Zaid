import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;

public class ChatServer {

    private static final int PORT = 9999;
    private static Set<ClientHandler> clientHandlers =
            Collections.synchronizedSet(new HashSet<>());
    private static ExecutorService pool = Executors.newCachedThreadPool();

    // 🔒 LEVEL 3 ADDITION: rate limiting
    private static final int MAX_CONNECTIONS_PER_IP = 3;
    private static Map<String, Integer> connectionCounts =
            new ConcurrentHashMap<>();
    // 🔒 END LEVEL 3 ADDITION

    public static void main(String[] args) {

        // 🔐 TLS keystore configuration (MANDATORY)
        System.setProperty("javax.net.ssl.keyStore", "server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        System.out.println("[SERVER] Starting TLS chat server on port " + PORT);

        try {
            SSLServerSocketFactory factory =
                    (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

            SSLServerSocket serverSocket =
                    (SSLServerSocket) factory.createServerSocket(PORT);

            System.out.println("[SERVER] Waiting for secure client connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();

                // 🔒 LEVEL 3 ADDITION: enforce rate limit
                connectionCounts.putIfAbsent(clientIP, 0);
                if (connectionCounts.get(clientIP) >= MAX_CONNECTIONS_PER_IP) {
                    System.out.println("[SERVER] Connection rejected from " + clientIP);
                    clientSocket.close();
                    continue;
                }
                connectionCounts.put(clientIP, connectionCounts.get(clientIP) + 1);
                

                System.out.println("[SERVER] Secure client connected: "
                        + clientSocket.getInetAddress());

                ClientHandler clientThread = new ClientHandler(clientSocket);
                clientHandlers.add(clientThread);
                pool.execute(clientThread);
            }

        } catch (IOException e) {
            System.err.println("[SERVER ERROR] " + e.getMessage());
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);

        // 🔒 LEVEL 3 ADDITION: decrement connection count
        String clientIP = client.socket.getInetAddress().getHostAddress();
        connectionCounts.put(clientIP, connectionCounts.get(clientIP) - 1);
        // 🔒 END LEVEL 3 ADDITION

        System.out.println("[SERVER] Client disconnected. Active clients: "
                + clientHandlers.size());
    }

    // Handles one client
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("ENTER_USERNAME");
                username = in.readLine();
                System.out.println("[SERVER] Username set: " + username);

                broadcast("[SERVER] " + username + " has joined the chat!", this);
                out.println("[SERVER] Welcome to the chat, " + username + "!");

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if (clientMessage.equalsIgnoreCase("/quit")) break;

                    String formattedMessage =
                            "[" + username + "]: " + clientMessage;
                    System.out.println("[BROADCAST] " + formattedMessage);
                    broadcast(formattedMessage, this);
                }

            } catch (IOException e) {
                System.err.println("[CLIENT ERROR] " + e.getMessage());
            } finally {
                try {
                    if (username != null) {
                        broadcast("[SERVER] " + username + " has left the chat.", this);
                    }
                    removeClient(this);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
