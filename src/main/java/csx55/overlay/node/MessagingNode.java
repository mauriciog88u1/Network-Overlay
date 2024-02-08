package csx55.overlay.node;

import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.Register;
import csx55.overlay.wireformats.RegisterResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import static csx55.overlay.util.DEBUG.debug_print;

public class MessagingNode implements Node {

    private TCPSender sender;
    private Socket registrySocket;
    private ServerSocket serverSocket;

    public MessagingNode(String registryHost, int registryPort) {
        debug_print("Initializing MessagingNode...");
        try {
            this.registrySocket = new Socket(registryHost, registryPort);
            debug_print("Connected to registry at " + registryHost + ":" + registryPort);
            this.sender = new TCPSender(registrySocket);

            this.serverSocket = new ServerSocket(0);
            debug_print("MessagingNode listening on port: " + serverSocket.getLocalPort());

            listenForConnections();
            regstriationToRegistry();
        } catch (IOException e) {
            debug_print("Error initializing MessagingNode: " + e.getMessage());
            System.err.println("Error initializing MessagingNode: " + e.getMessage());
        }
    }

    private void listenForConnections() {

        System.out.printf("Listening for connections on %s:%d%n", getHostname(), getPort());
        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    debug_print("Accepted connection from " + clientSocket.getRemoteSocketAddress());
                    new TCPReceiverThread(clientSocket, this).start();
                }
            } catch (IOException e) {
                debug_print("Error accepting connection: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof RegisterResponse) {
            RegisterResponse response = (RegisterResponse) event;
            if (response.getStatusCode() == 1) {
                System.out.println(response.getAdditionalInfo());
                debug_print("Registration successful: " + response.getAdditionalInfo());
            } else {
                debug_print("Registration failed: " + response.getAdditionalInfo());
            }
        }
    }

    @Override
    public String getHostname() {
        return serverSocket.getInetAddress().getHostName();
    }

    @Override
    public String getIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            debug_print("Error getting IP address: " + e.getMessage());
            return "unknown";
        }
    }

    @Override
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void handleNewConnection(Socket clientSocket) {
        debug_print("Handling new connection from: " + clientSocket.toString());
        new TCPReceiverThread(clientSocket, this).start();
    }

    public void regstriationToRegistry() throws IOException {
        InetAddress localHost = InetAddress.getLocalHost();
        debug_print(getHostname());
        Register register = new Register(localHost.getHostName(),localHost.getHostAddress(), serverSocket.getLocalPort());
        debug_print("Sending "+ Arrays.toString(register.getBytes()) + " to registry.");
        sender.sendMessage(register.getBytes());
        debug_print("Registration message sent to registry.");
    }

/**
 * Computers on the domain can use hostname whereas computers on the same network can use ip address
 */
    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            debug_print("Usage: java MessagingNode <registry host> <registry port> [--DEBUG]");
            return;
        }
        if (args.length == 3 && (args[2].equals("--DEBUG"))) {
            DEBUG.DEBUG = true;
        }
        new MessagingNode(args[0], Integer.parseInt(args[1]));
    }
}
