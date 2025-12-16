import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

public class ChatClient {

    private SSLSocket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private String username;

    public ChatClient(String serverAddress, int port) {
        try {
            // 🔐 Create TLS socket (REPLACES new Socket)
            SSLSocketFactory factory =
                    (SSLSocketFactory) SSLSocketFactory.getDefault();

            socket = (SSLSocket) factory.createSocket(serverAddress, port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            scanner = new Scanner(System.in);

            System.out.println("Connected securely to chat server at "
                    + serverAddress + ":" + port);

            new Thread(new IncomingReader()).start();

            System.out.print("Enter username: ");
            username = scanner.nextLine();
            out.println(username);

            sendMessages();

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private void sendMessages() {
        while (true) {
            String message = scanner.nextLine();
            if (message.equalsIgnoreCase("/quit")) {
                out.println("/quit");
                break;
            }
            out.println(message);
        }

        try {
            socket.close();
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        scanner.close();
    }

    private class IncomingReader implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
            }
        }
    }

    public static void main(String[] args) {

        // 🔐 TLS truststore config (MUST be here)
        System.setProperty("javax.net.ssl.trustStore", "server.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        String serverAddress = "localhost";
        int port = 9999;

        if (args.length >= 1) {
            serverAddress = args[0];
        }
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }

        new ChatClient(serverAddress, port);
    }
}
