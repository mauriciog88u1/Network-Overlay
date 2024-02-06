package csx55.overlay.node;

import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.Register;

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
            registerWithRegistry();
        } catch (IOException e) {
            debug_print("Error initializing MessagingNode: " + e.getMessage());
        }
    }

    private void listenForConnections() {

        System.out.println("Listening for connections on " +getHostname()+ ":" + getPort());
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
        debug_print("Event received: processing...");
        debug_print(event.toString());
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

    public void registerWithRegistry() throws IOException {
        InetAddress localHost = InetAddress.getLocalHost();
        Register register = new Register(localHost.getHostAddress(), serverSocket.getLocalPort());
        debug_print("Sending "+ Arrays.toString(register.getBytes()) + " to registry.");
        sender.sendMessage(register.getBytes());
        debug_print("Registration message sent to registry.");
    }

    public static void main(String[] args) {
        System.out.println("MessagingNode starting...");
        debug_print("MessagingNode starting...");
        if (args.length < 2) {
            debug_print("Usage: java MessagingNode <registry host> <registry port> [--DEBUG]");
            return;
        }
        if (args.length == 3 && (args[2].equals("--DEBUG"))) {
            DEBUG.DEBUG = true;
        }
        new MessagingNode(args[0], Integer.parseInt(args[1]));
    }
}
