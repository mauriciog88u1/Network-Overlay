package csx55.overlay.node;

import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.Register;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static csx55.overlay.util.DEBUG.DEBUG;
import static csx55.overlay.util.DEBUG.debug_print;

public class MessagingNode implements Node {
    private TCPSender sender;
    private Socket registrySocket;
    private ServerSocket serverSocket;

    public MessagingNode(String registryHost, int registryPort) {
        debug_print("MessagingNode created with registry at: " + registryHost + " " + registryPort);
        try {
            this.registrySocket = new Socket(registryHost, registryPort);
            this.sender = new TCPSender(registrySocket);
            debug_print("Connected to registry at: " + registryHost + ":" + registryPort);

            this.serverSocket = new ServerSocket(0); // Dynamically allocate a port
            debug_print("MessagingNode listening on port: " + serverSocket.getLocalPort());
        } catch (Exception e) {
            debug_print("Error initializing MessagingNode: " + e.getMessage());
        }
    }

    @Override
    public void onEvent(Event event) {
        // Implement event processing logic here
    }

    @Override
    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            debug_print("Error getting hostname: " + e.getMessage());
            return "unknown";
        }
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
        TCPReceiverThread receiverThread = new TCPReceiverThread(clientSocket, this);
        receiverThread.start();
    }

    public void registerWithRegistry() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            Register register = new Register(localHost.getHostAddress(), serverSocket.getLocalPort());
            sender.sendMessage(register.getBytes());
            debug_print("Registration message sent: " + localHost.getHostAddress() + ":" + serverSocket.getLocalPort());
        } catch (Exception e) {
            debug_print("Error registering with registry: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("MessagingNode starting");
        if (args.length != 2 && args.length != 3) {
            System.out.println("Usage: java MessagingNode <registry host> <registry port> [--DEBUG]");
            return;
        }
        if (args.length == 3 && args[2].equalsIgnoreCase("--DEBUG")) {
            DEBUG = true;
            debug_print("DEBUG mode enabled");
        }
        String registryHost = args[0];
        int registryPort = Integer.parseInt(args[1]);
        MessagingNode messagingNode = new MessagingNode(registryHost, registryPort);
        messagingNode.registerWithRegistry();
    }
}
