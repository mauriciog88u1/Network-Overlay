package csx55.overlay.node;

import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.transport.TCPServerThread;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.util.OverlayCreator;
import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
                if (tokens.length != 2) {
                    System.out.println("Usage: setup-overlay <number-of-connections>");
                    return;
                }
                setupOverlay(Integer.parseInt(tokens[1]));
                break;
            case "send-overlay-link-weights":
                sendOverlayLinkWeights();
                break;
            default:
                String usage = "Usage: list-messaging-nodes | list-weights | setup-overlay <number-of-connections> | send-overlay-link-weights";
                System.out.println("Unknown command: " + command + "\n" + usage);
                break;
        }
    }

    private void listMessagingNodes() {
        registeredNodes.forEach((key, value) -> debug_print(key + " -> " + value.toString()));
    }

    private void listWeights() {
        overlayCreator.getOverlayMap().forEach((node, connections) -> System.out.println(node + " -> " + connections));

    }


    public synchronized void setupOverlay(int numberOfConnections) {
        if (registeredNodes.size() < numberOfConnections + 1) { // +1 because a node cannot connect to itself
            System.err.println("Error: Not enough nodes registered to create an overlay with " + numberOfConnections + " connections per node.");
            return;
        }

        OverlayCreator overlayCreator = new OverlayCreator();
        ConcurrentHashMap<String, List<String>> overlay = overlayCreator.createOverlay(registeredNodes, numberOfConnections);

        overlay.forEach((nodeKey, connections) -> {
            try {
                NodeWrapper node = registeredNodes.get(nodeKey);
                List<String> connectionInfoList = connections.stream()
                        .map(connectionKey -> {
                            NodeWrapper connectionNode = registeredNodes.get(connectionKey);
                            return connectionNode.getHostname() + ":" + connectionNode.getPort();
                        })
                        .collect(Collectors.toList());

                MessagingNodesList message = new MessagingNodesList(connectionInfoList.size(), connectionInfoList);
                sendMessagingNodesList(node.getIp(), node.getPort(), message);
            } catch (IOException e) {
                System.err.println("Error sending MESSAGING_NODES_LIST to " + nodeKey + ": " + e.getMessage());
            }
        });

        System.out.println("Overlay setup complete. Each node is connected to " + numberOfConnections + " other nodes.");
    }

    private void sendOverlayLinkWeights() {
        DEBUG.debug_print("Inside sendOverlayLinkWeights...");
        LinkWeights linkWeights = new LinkWeights();
        ConcurrentHashMap<String, List<String>> overlay = overlayCreator.getOverlayMap();
        linkWeights.generateLinkWeights(overlay);

        try {
            byte[] message = linkWeights.getBytes();

            for (NodeWrapper node : registeredNodes.values()) {
                TCPSender sender = new TCPSender(new Socket(node.getIp(), node.getPort()));
                sender.sendMessage(message);
                sender.closeConnection();
            }

          DEBUG.debug_print("Link weights sent to all nodes.");
        } catch (IOException e) {
            DEBUG.debug_print("Error sending link weights: " + e.getMessage());
            System.err.println("Error sending link weights: " + e.getMessage());
        }
    }

    private void sendMessagingNodesList(String ip, int port, MessagingNodesList message) throws IOException {
        try {
            TCPSender sender = new TCPSender(new Socket(ip, port));
            sender.sendMessage(message.getBytes());
        }
        catch (IOException e) {
            System.err.println("Error sending MESSAGING_NODES_LIST to " + ip + ":" + port + ": " + e.getMessage());
        }
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
                DEBUG.debug_print("Trying to send message to " + node.getHostname() + " at " + node.getIp() + ":" + node.getPort());
                sender.sendMessage(response.getBytes());
                sender.closeConnection();
            } catch (IOException e) {
                debug_print("Error sending register response: " + e.getMessage());
            }
        }
    }

    public synchronized void deregisterNode(String hostname, String ip, int port) {
        String key = hostname + ":" + port;
        if (registeredNodes.containsKey(key)) {
            registeredNodes.remove(key);
            debug_print("Node deregistered: " + key);
            sendDeregisterResponse(key, (byte)1, "Node successfully deregistered.");
        } else {
            debug_print("Node not found for deregistration: " + key);
            sendDeregisterResponse(key, (byte)0, "Node not found for deregistration.");
        }
    }

    private void sendDeregisterResponse(String key, byte status, String message) {
        NodeWrapper node = registeredNodes.get(key);
        if (node != null) {
            try {
                TCPSender sender = new TCPSender(new Socket(node.getIp(), node.getPort()));
                DeregisterResponse response = new DeregisterResponse(status, message);
                sender.sendMessage(response.getBytes());
                sender.closeConnection();
            } catch (IOException e) {
                debug_print("Error sending deregister response: " + e.getMessage());
            }
        }
    }
    private void startMessageSending(int rounds) {
        TaskInitiate taskInitiate = new TaskInitiate(rounds);
        try {
            byte[] message = taskInitiate.getBytes();
            for (NodeWrapper node : registeredNodes.values()) {
                TCPSender sender = new TCPSender(new Socket(node.getIp(), node.getPort()));
                sender.sendMessage(message);
                sender.closeConnection();
            }
            System.out.println("Initiated message sending for " + rounds + " rounds.");
        } catch (IOException e) {
            System.err.println("Error initiating message sending: " + e.getMessage());
        }
    }

    @Override
    public void onEvent(Event event) {
        debug_print("Registry received event of type: " + event.getType());
        // Handling Register events
        if (event instanceof Register) {
            Register registerEvent = (Register) event;
            String ipAddress = registerEvent.getIpAddress();
            int port = registerEvent.getPort();
            String hostname = registerEvent.getHostname();
            registerNode(hostname, ipAddress, port);
        }
        else if (event instanceof Deregister) {
            Deregister deregEvent = (Deregister) event;
            String ipAddress = deregEvent.getIpAddress();
            int port = deregEvent.getPort();
            String hostname = deregEvent.getHostname();
            deregisterNode(hostname, ipAddress, port);
        }
        else {
            System.err.println("Unknown event type: " + event.getType());
            debug_print("Unknown event type: " + event.getType());
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

    @Override
    public void handleNewConnection(Socket clientSocket) {
        new TCPReceiverThread(clientSocket, this).start();
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
