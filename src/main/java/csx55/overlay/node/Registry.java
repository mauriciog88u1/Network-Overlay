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
    private final ConcurrentHashMap<String, NodeWrapper> registeredNodes;
    private final OverlayCreator overlayCreator;
    private final int serverPort;
    private String hostname;
    private String ip;

    public Registry(int serverPort) {
        this.serverPort = serverPort;
        initializeServerDetails();
        serverThread = new TCPServerThread(serverPort, this);
        registeredNodes = new ConcurrentHashMap<>();
        overlayCreator = new OverlayCreator();
    }

    private void initializeServerDetails() {
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
            this.hostname = InetAddress.getLocalHost().getHostName();
            debug_print("Initializing Registry on port: " + serverPort + ", IP: " + ip + ", Hostname: " + hostname);
        } catch (UnknownHostException e) {
            debug_print("Could not determine host IP or hostname: " + e.getMessage());
            this.ip = "unknown";
            this.hostname = "unknown";
        }
    }

    public void start() {
        debug_print("Starting Registry server thread on hostname: " + hostname + ", ip: " + ip + ", port: " + serverPort);
        serverThread.start();
        listenForCommands();
    }

    private void listenForCommands() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Registry listening for commands...");
        String command;
        try {
            while ((command = reader.readLine()) != null) {
                processCommand(command);
            }
        } catch (IOException e) {
            debug_print("Error reading command: " + e.getMessage());
        }
    }

    private void processCommand(String command) {
        String[] tokens = command.split("\\s+");
        debug_print("Processing command: " + command);
        switch (tokens[0]) {
            case "list-messaging-nodes":
                listMessagingNodes();
                break;
            case "list-weights":
                listWeights();
                break;
            case "setup-overlay":
                int numConnections = Integer.parseInt(tokens[1]);
                setupOverlay(numConnections);
                break;
            case "send-overlay-link-weights":
                sendOverlayLinkWeights();
                break;
            default:
                System.out.println("Unknown command: " + tokens[0]);
                break;
        }
    }

    private void listMessagingNodes() {
        registeredNodes.forEach((key, value) -> debug_print(key + " -> " + value.toString()));
    }

    private void listWeights() {
        debug_print("Overlay weights listing is not implemented.");
    }

    private void setupOverlay(int numberOfConnections) {
        debug_print("Overlay setup with " + numberOfConnections + " connections per node is not implemented.");
    }

    private void sendOverlayLinkWeights() {
        debug_print("Sending overlay link weights is not implemented.");
    }

    public synchronized void registerNode(String hostname, String ip, int port) {
        String key = hostname + ":" + port;
        if (!registeredNodes.containsKey(key)) {
            NodeWrapper newNode = new NodeWrapper(hostname, ip, port);
            registeredNodes.put(key, newNode);
            debug_print("Node registered: " + newNode);
            sendRegisterResponse(newNode, true, "Registration request\n" +
                    "successful. The number of messaging nodes currently constituting the overlay is (" + registeredNodes.size() + ")");
        } else {
            debug_print("Attempt to register already registered node: " + key);
            sendRegisterResponse(null, false, "Node already registered.");
        }
    }

    private void sendRegisterResponse(NodeWrapper node, boolean success, String message) {
        byte status = success ? (byte)1 : (byte)0;
        RegisterResponse response = new RegisterResponse(status, message);
        if (node != null) {
            try {
                TCPSender sender = new TCPSender(new Socket(node.getIp(), node.getPort()));
                sender.sendMessage(response.getBytes());
                sender.closeConnection();
            } catch (IOException e) {
                debug_print("Error sending register response: " + e.getMessage());
            }
        }
    }

    public synchronized void deregisterNode(String hostname, int port) {
        String key = hostname + ":" + port;
        if (registeredNodes.remove(key) != null) {
            debug_print("Node deregistered: " + key);
        } else {
            debug_print("Attempt to deregister non-existent node: " + key);
        }
    }

    @Override
    public void onEvent(Event event) {
        debug_print("Registry received event: " + event.getType());
        if (event instanceof Register) {
            handleRegisterEvent((Register) event);
        }
    }

    private void handleRegisterEvent(Register registerEvent) {
        registerNode(registerEvent.getHostname(), registerEvent.getIpAddress(), registerEvent.getPort());
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public int getPort() {
        return serverPort;
    }

    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2) {
            System.out.println("Usage: java csx55.overlay.node.Registry <port-number> [--DEBUG]");
            return;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("--DEBUG")) {
            DEBUG.DEBUG = true;
        }
        int port = Integer.parseInt(args[0]);
        Registry registry = new Registry(port);
        registry.start();
    }
}
