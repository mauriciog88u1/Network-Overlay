package csx55.overlay.node;

import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.transport.TCPServerThread;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.util.OverlayCreator;
import csx55.overlay.util.DEBUG;
import csx55.overlay.util.StatisticsCollectorAndDisplay;
import csx55.overlay.wireformats.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private Map<String, Integer> linkWeightsMap;
    private final Set<String> completedNodes = ConcurrentHashMap.newKeySet();

    private final StatisticsCollectorAndDisplay statisticsCollector = new StatisticsCollectorAndDisplay();


    private int number_of_rounds =3;


    public Registry(int serverPort) {
        this.serverPort = serverPort;
        initializeServerDetails();
        serverThread = new TCPServerThread(serverPort, this);
        registeredNodes = new ConcurrentHashMap<>();
        overlayCreator = new OverlayCreator();
        linkWeightsMap = new HashMap<>();
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
        System.out.println("Registry started on " + hostname + ":" + serverPort );
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
        switch (tokens[0].strip()) {
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
            case "start-number-of-rounds":
                startMessageSending(number_of_rounds);
                break;
            default:
                String usage = "Usage: list-messaging-nodes | list-weights | setup-overlay <number-of-connections> | send-overlay-link-weights | start-number-of-rounds";
                System.out.println("Unknown command: " + command + "\n" + usage);
                break;
        }
    }

    private void listMessagingNodes() {
        if (registeredNodes.isEmpty()) {
            System.out.println("No messaging nodes registered.");
            return;
        }

        for (NodeWrapper node : registeredNodes.values()) {
            System.out.println(String.format("%s %d", node.getHostname(), node.getPort()));
        }
    }

    private void listWeights() {
        String format = "%s %s %d";
        if (linkWeightsMap.isEmpty()) {
            System.out.println("Setup overlay and send link weights first.");
        } else {
            linkWeightsMap.forEach((link, weight) -> {
                String[] nodes = link.split("@");
                System.out.println(String.format(format, nodes[0], nodes[1], weight));
             
            });
        }
    }
    


    public synchronized void setupOverlay(int numberOfConnections) {
        if (registeredNodes.size() < numberOfConnections + 1) { // +1 because a node cannot connect to itself
            System.err.println("Error: Not enough nodes registered to create an overlay with " + numberOfConnections + " connections per node.");
            return;
        }

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
        if (overlay == null || overlay.isEmpty()) {
            debug_print("Overlay not set up yet. Cannot send link weights.");
            return;
        }
        linkWeights.generateLinkWeights(overlay);
        this.linkWeightsMap = linkWeights.getLinkweights();
        System.out.println(linkWeights.toString());
    
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
        else if(event instanceof TaskComplete){
            TaskComplete complete = (TaskComplete) event;
            String ipAddress = normalizeHostnameToFQDN(complete.getNodeIPAddress());
            int portNum = complete.getNodePort();
           handleTaskComplete(ipAddress,portNum);
        } else if (event instanceof TaskSummaryResponse) {
            handleTaskSummaryResponse((TaskSummaryResponse)event);


        } else {
            System.err.println("Unknown event type: " + event.getType());
        }
    }


    private void handleTaskComplete(String hostname, int port) {
        String nodeIdentifier = hostname + ":" + port;
    debug_print("Task completion reported by " + nodeIdentifier +"----------------------------------");
        if (registeredNodes.containsKey(nodeIdentifier)) {
            completedNodes.add(normalizeHostnameToFQDN(nodeIdentifier));
            debug_print("Task completion reported by " + nodeIdentifier);

            if (completedNodes.size() == registeredNodes.size()) {
                try {
                    sendTaskSummaryRequest();
                } catch (IOException e) {
                    System.err.println("Error sending Task Summary Request: " + e.getMessage());
                }
            }
        } else {
            debug_print("Received task completion from an unregistered node: " + nodeIdentifier);
        }
    }


    private void sendTaskSummaryRequest() throws IOException {
        TaskSummaryRequest taskSummaryRequest = new TaskSummaryRequest();
        byte[] message = taskSummaryRequest.getBytes();
        try {
            DEBUG.debug_print("Waiting 15 seconds before sending Task Summary Request...");
            Thread.sleep(15000); // 15 secound wait time
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (NodeWrapper node : registeredNodes.values()) {
            try {

                Socket socket = new Socket(node.getIp(), node.getPort());
                TCPSender sender = new TCPSender(socket);
                sender.sendMessage(message);
                debug_print("Sent Task Summary Request to " + node.getIp() + ":" + node.getPort());
            }

            catch (IOException e) {
                System.err.println("Error sending Task Summary Request to " + node.getIp() + ":" + node.getPort() + ": " + e.getMessage());
            }
        }
    }
    private void handleTaskSummaryResponse(TaskSummaryResponse taskSummaryResponse) {
        String nodeIdentifier = taskSummaryResponse.getNodeIP() + ":" + taskSummaryResponse.getNodePort();
            int sendMessages =taskSummaryResponse.getSentMessages();
            int receivedMessages = taskSummaryResponse.getReceivedMessages();
            long sentSum = taskSummaryResponse.getSummationOfSentMessages();
            long receivedSum = taskSummaryResponse.getSummationOfReceivedMessages();
            int relayedMessages = taskSummaryResponse.getRelayedMessages();

            statisticsCollector.addNodeStatistics(nodeIdentifier, sentSum, receivedSum, relayedMessages,sendMessages,receivedMessages);

            if (statisticsCollector.size() == completedNodes.size()) {
                statisticsCollector.displaySummary();
            }
    }
    private String normalizeHostnameToFQDN(String hostPort) {
        try {
            String hostname = hostPort.contains(":") ? hostPort.substring(0, hostPort.indexOf(":")) : hostPort;
            InetAddress addr = InetAddress.getByName(hostname);

            String fqdn = addr.getCanonicalHostName();
            return fqdn;
        } catch (UnknownHostException e) {
            debug_print("Failed to normalize hostname to FQDN: " + hostPort + ", error: " + e.getMessage());
            return hostPort;
        }
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
