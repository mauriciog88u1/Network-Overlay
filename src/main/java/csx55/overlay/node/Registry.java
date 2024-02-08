package csx55.overlay.node;

import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.transport.TCPServerThread;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.util.OverlayCreator;
import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.Register;
import csx55.overlay.wireformats.RegisterResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import static csx55.overlay.util.DEBUG.debug_print;

public class Registry implements Node {
    private final TCPServerThread serverThread;

    private TCPSender sender;
    private final ConcurrentHashMap<String, NodeWrapper> registeredNodes; // Changed to NodeWrapper
    private final OverlayCreator overlayCreator;
    private final int serverPort;
    private String hostname;
    private String ip;

    public Registry(int serverPort) {
        this.serverPort = serverPort;
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            debug_print("Could not determine host IP or hostname: " + e.getMessage());
            this.ip = "unknown";
            this.hostname = "unknown";
        }
        debug_print("Initializing Registry on port: " + serverPort + ", IP: " + ip + ", Hostname: " + hostname);
        serverThread = new TCPServerThread(serverPort, this);
        registeredNodes = new ConcurrentHashMap<>();
        overlayCreator = new OverlayCreator();
    }

    public void start() {
        debug_print("Starting Registry server thread. on hostname: " + hostname + " ip: " + ip + " port: " + serverPort);
        serverThread.start();
        listenForCommands();
    }

    private void listenForCommands() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Registry listening for commands...");
        while (true) {
            try {
                String command = reader.readLine();
                if (command != null) {
                    processCommand(command);
                }
            } catch (IOException e) {
                System.out.println("Error reading command: " + e.getMessage());
            }

        }
    }

    private void processCommand(String command) {
        String[] tokens = command.split(" ");
        debug_print("Processing command: " + command);
        switch (tokens[0]) {
            case "list-messaging-nodes":
                listMessagingNodes();
                break;
            case "list-weights":
                listWeights();
                break;
            case "setup-overlay":
                if (tokens.length > 1) {
                    setupOverlay(Integer.parseInt(tokens[1]));
                } else {
                    System.out.println("Error: setup-overlay command requires a number of connections parameter.");
                }
                break;
            case "send-overlay-link-weights":
                sendOverlayLinkWeights();
                break;
            default:
                System.out.println("Error: unknown command \"" + tokens[0] + "\"");
        }
    }

    private void listMessagingNodes() {
        if (registeredNodes.isEmpty()) {
            System.out.println("No nodes are currently registered.");
        } else {
            for (NodeWrapper node : registeredNodes.values()) {
                System.out.println(node);
            }
        }
    }

    private void listWeights() {
//        if (overlayCreator.getLinkWeights().isEmpty()) {
//            System.out.println("No link weights are currently available.");
//        } else {
//            for (Link link : overlayCreator.getLinkWeights().keySet()) {
//                System.out.println(link + " weight: " + overlayCreator.getLinkWeights().get(link));
//            }
//        }

    }

    private void setupOverlay(int numberOfConnections) {
        // Implement overlay setup logic, using numberOfConnections as needed
        System.out.println("Setting up overlay with " + numberOfConnections + " connections per node... (implement as needed)");
    }

    private void sendOverlayLinkWeights() {
        // Implement logic for sending overlay link weights to all nodes
        System.out.println("Sending overlay link weights... (implement as needed)");
    }

    public synchronized void registerNode(String hostname, String ip, int port) throws RuntimeException, IOException {
        String key = hostname + ":" + port;
        String successMessage =" “Registration request\n" +
                "successful. The number of messaging nodes currently constituting the overlay is (";
        byte success = 1;
        if (!registeredNodes.containsKey(key)) {
            NodeWrapper nodeWrapper = new NodeWrapper(hostname, ip, port);
            registeredNodes.put(key, nodeWrapper);
            RegisterResponse response = new RegisterResponse(success, successMessage + registeredNodes.size() + ").");
            try {
                sender.sendMessage(response.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            debug_print("Node registered: " + nodeWrapper);
        } else {
            success = 0;
            RegisterResponse response = new RegisterResponse(success, "Node already registered.");
            sender.sendMessage(response.getBytes());
            debug_print("Node already registered: " + hostname + ":" + port);
        }
    }

    public synchronized void deregisterNode(String hostname, int port) {
        String key = hostname + ":" + port;
        if (registeredNodes.containsKey(key)) {
            registeredNodes.remove(key);
            debug_print("Node deregistered: " + hostname + ":" + port);
        } else {
            debug_print("Node not found for deregistration: " + hostname + ":" + port);
        }
    }

    @Override
    public void onEvent(Event event)  {
        debug_print("Registry received an event of type: " + event.getType());
        if (event instanceof Register) {
            Register registerEvent = (Register) event;
            String ipAddress = registerEvent.getIpAddress();
            String hostname = registerEvent.getHostname();
            int port = registerEvent.getPort();
            try {
                registerNode(hostname,ipAddress,port);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        }
}



    @Override
    public String getHostname() {
        return this.hostname;
    }

    @Override
    public String getIp() {
        return this.ip;
    }

    @Override
    public int getPort() {
        return this.serverPort;
    }
    @Override
    public void handleNewConnection(Socket clientSocket) {
        debug_print("Handling new connection in Registry: " + clientSocket);
        TCPReceiverThread receiverThread = new TCPReceiverThread(clientSocket, this);
        receiverThread.start();

    }



    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2) {
            System.out.println("Usage: java csx55.overlay.node.Registry <port-number> [--DEBUG]");
            return;
        }
        if (args.length == 2 && (args[1].equals("--DEBUG"))) {
            DEBUG.DEBUG = true;
        }

        int port = Integer.parseInt(args[0]);
        Registry registry = new Registry(port);
        registry.start();
    }
}
