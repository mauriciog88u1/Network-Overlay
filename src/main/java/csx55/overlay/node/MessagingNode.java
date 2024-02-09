package csx55.overlay.node;

import csx55.overlay.dijkstra.ShortestPath;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import static csx55.overlay.util.DEBUG.debug_print;

public class MessagingNode implements Node {
    private TCPSender sender;
    private ServerSocket serverSocket;

    private int sendTracker = 0;
    private int receiveTracker = 0;

    public MessagingNode(String registryHost, int registryPort) {
        try {
            Socket registrySocket = new Socket(registryHost, registryPort);
            this.sender = new TCPSender(registrySocket);
            this.serverSocket = new ServerSocket(0); // Dynamically allocate a port

            debug_print("Connected to registry at " + registryHost + ":" + registryPort);
            debug_print("MessagingNode listening on port: " + serverSocket.getLocalPort());

            listenForConnections();
            registrationToRegistry();
            setupShutdownHook();
        } catch (IOException e) {
            debug_print("Error initializing MessagingNode: " + e.getMessage());
        }
    }

    private void listenForConnections() {
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

    private void registrationToRegistry() throws IOException {
        InetAddress localHost = InetAddress.getLocalHost();
        Register register = new Register(localHost.getHostName(), localHost.getHostAddress(), serverSocket.getLocalPort());
        sender.sendMessage(register.getBytes());
        debug_print("Registration message sent to registry.");
    }

    private void deregisterFromRegistry() {
        try {
            Deregister deregister = new Deregister(getIp(), getPort());
            sender.sendMessage(deregister.getBytes());
            debug_print("Deregistration request sent to registry.");
        } catch (IOException e) {
            debug_print("Error sending deregistration request: " + e.getMessage());
        }
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::deregisterFromRegistry));
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof RegisterResponse) {
            handleRegisterResponse((RegisterResponse) event);
        } else if (event instanceof MessagingNodesList) {
            handleMessagingNodesList((MessagingNodesList) event);
        } else if (event instanceof LinkWeights) {
            handleLinkWeights((LinkWeights) event);
        } else if (event instanceof TaskInitiate) {
            handleTaskInitiate((TaskInitiate) event);
        }
    }
    private void handleTaskInitiate(TaskInitiate event) {
      debug_print("Received task initiate message from registry: " + event.getRounds() + " rounds");

    }
    private void handleLinkWeights(LinkWeights event) {
        debug_print("Received link weights from registry:");
        System.out.println("“Link weights received and processed. Ready to send messages.");
        event.getLinkweights().forEach((k, v) -> debug_print(k + " -> " + v));

    }

    private void handleMessagingNodesList(MessagingNodesList event) {
        List<String> nodeInfoList = event.getMessagingNodesInfo();
        debug_print("Received list of messaging nodes to connect to:");
        for (String nodeInfo : nodeInfoList) {
            debug_print(nodeInfo);
        }

    }

    private void handleRegisterResponse(RegisterResponse response) {
        if (response.getStatusCode() == 1) {
            debug_print("Registration successful: " + response.getAdditionalInfo());
        } else {
            debug_print("Registration failed: " + response.getAdditionalInfo());
        }
    }

    @Override
    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            return "unknown";
        }
    }

    @Override
    public String getIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            return "unknown";
        }
    }

    @Override
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void handleNewConnection(Socket clientSocket) {
        new TCPReceiverThread(clientSocket, this).start();
    }
    private void processCommand(String command) {
                switch (command) {
                    case "print-shortest-path":
                        printShortestPath();
                        break;
                    case "exit-overlay":
                        exitOverlay();
                        break;
                    default:
                        DEBUG.debug_print("Unknown command: " + command);
                        String usage = "Usage: print-shortest-path | exit-overlay";
                        System.out.println(usage);
                }



        }

    private void exitOverlay() {
        debug_print("Exiting overlay... for node " + getHostname());
        deregisterFromRegistry();
        System.exit(0);
    }

    private void printShortestPath() {
        ShortestPath shortestPath = new ShortestPath();
        debug_print("Shortest path to all nodes:");
    }

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Usage: java MessagingNode <registry host> <registry port> [--DEBUG]");
            return;
        }
        if (args.length == 3 && args[2].equals("--DEBUG")) {
            DEBUG.DEBUG = true;
        }
        try {
            new MessagingNode(args[0], Integer.parseInt(args[1]));
        } catch (Exception e) {
            System.err.println("MessagingNode failed to start: " + e.getMessage());
        }
    }
}
