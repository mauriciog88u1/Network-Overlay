package csx55.overlay.node;

import csx55.overlay.dijkstra.RoutingCache;
import csx55.overlay.dijkstra.ShortestPath;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static csx55.overlay.util.DEBUG.debug_print;

public class MessagingNode implements Node {
    private TCPSender sender;
    private ServerSocket serverSocket;
    private String registry_hostname;
    private int registry_port;

    private final AtomicInteger sendTracker = new AtomicInteger(0);
    private final AtomicInteger receiveTracker = new AtomicInteger(0);
    private final AtomicInteger relayTracker = new AtomicInteger(0);
    private final AtomicLong sendSummation = new AtomicLong(0);
    private final AtomicLong receiveSummation = new AtomicLong(0);

    private ConcurrentHashMap <String, Map<String, Integer>> networkTopology;

    private RoutingCache routingCache;



    public MessagingNode(String registryHost, int registryPort) {
        try {
            Socket registrySocket = new Socket(registryHost, registryPort);
            this.sender = new TCPSender(registrySocket);
            this.serverSocket = new ServerSocket(0); // Dynamically allocate a port
            this.routingCache = new RoutingCache();
            this.networkTopology = new ConcurrentHashMap<>();
            this.registry_hostname = registryHost;
            this.registry_port = registry_port;


            debug_print("Connected to registry at " + registryHost + ":" + registryPort);
            debug_print("MessagingNode listening on port: " + serverSocket.getLocalPort());

            listenForConnections();
            registrationToRegistry();
            listenForCommands();
            setupShutdownHook();
        } catch (IOException e) {
            debug_print("Error initializing MessagingNode: " + e.getMessage());
        }
    }

    private void listenForCommands() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Messaging Node listening for commands...");
        String command;
        try {
            while ((command = reader.readLine()) != null) {
                processCommand(command);
            }
        } catch (IOException e) {
            debug_print("Error reading command: " + e.getMessage());
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
        Register register = new Register(getHostname(), getIp(), serverSocket.getLocalPort());
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
        switch (event.getType()) {
            case Protocol.REGISTER_RESPONSE:
                handleRegisterResponse((RegisterResponse) event);
                break;
            case Protocol.MESSAGING_NODES_LIST:
                handleMessagingNodesList((MessagingNodesList) event);
                break;
            case Protocol.LINK_WEIGHTS:
                handleLinkWeights((LinkWeights) event);
                break;
            case Protocol.TASK_INITIATE:
                handleTaskInitiate((TaskInitiate) event);
                break;
            case Protocol.MESSAGE:
                handleReceivedMessage((Message) event);
                break;
            case Protocol.PULL_TRAFFIC_SUMMARY:
                handlePullTrafficSummary((TaskSummaryRequest) event);
                break;

            default:
                System.out.println("Unknown event type: " + event.getType());
                break;
        }
    }

    private synchronized void handlePullTrafficSummary(TaskSummaryRequest event) {
        debug_print("Received traffic summary request. Sending summary.. " + event.getType());
        try {
            TaskSummaryResponse response = new TaskSummaryResponse(getHostname(), getPort(), sendSummation.get(), receiveSummation.get(), relayTracker.get(),sendTracker.get(),receiveTracker.get());
            sender.sendMessage(response.getBytes());
            debug_print("Sending Traffic Summary for " + getHostname());
        } catch (IOException e) {
            System.out.println("Error sending traffic summary: " + e.getMessage());
            System.exit(1);
        } finally {
            debug_print(String.format("Resetting counters: counters old values : Sendsummation: %d, receiveSummation: %d, relayTrack: %d", sendSummation.get(), receiveSummation.get(), relayTracker.get()));
            sendSummation.set(0);
            receiveSummation.set(0);
            relayTracker.set(0);
        }
    }


    private synchronized void handleReceivedMessage(Message event) {
        debug_print("[DEBUG]Received message: " + event.getPayload() + " from " + event.getRroutingTable().get(0));
        receiveTracker.incrementAndGet();
        if (event.getRroutingTable().size() > 1) {
            relayTracker.incrementAndGet();

            String nextHopIdentifier = event.getRroutingTable().get(0);
            event.getRroutingTable().remove(0);
            sendMessageToNextHop(nextHopIdentifier, event);
        } else {
            receiveSummation.addAndGet(event.getPayload());
            debug_print("Sink node has recieved messages" +receiveSummation);
        }
    }

    private synchronized void handleTaskInitiate(TaskInitiate event) {
        Random random = new Random();
        for (int i = 0; i < event.getRounds(); i++) {
            if (networkTopology.isEmpty()) {
                debug_print("Network topology is empty. Cannot select a destination.");
                continue;
            }

            String destination = selectRandomDestination();
            if (destination == null) {
                debug_print("Failed to select a destination. Skipping this round.");
                continue;
            }
            computeAndCacheShortestPath(destination);
            List<String> path = ensurePathIsFound(destination);
            if (!path.isEmpty()) {
                debug_print("Sending message to " + destination + " along path: " + path);
                sendMessageAlongPath(path, random);
            } else {
                debug_print("Failed to find path to " + destination);
            }
        }
        sendTaskComplete();
    }

    private String selectRandomDestination() {
        List<String> keys = new ArrayList<>(networkTopology.keySet());
        if (keys.isEmpty()) return null;
        return keys.get(new Random().nextInt(keys.size()));
    }

    public synchronized void computeAndCacheShortestPath(String destination) {
        String source = normalizeHostnameToFQDN(getHostname()) + ":" + getPort();
        debug_print("Attempting to find key in networkTopology: " + source);
        debug_print("Network topology keyset: " + networkTopology.keySet());
        if (networkTopology.isEmpty()) {
            debug_print("Network topology is empty.");
            return;
        }
        if (!networkTopology.containsKey(source)) {
            debug_print("Source node is missing in topology. Available keys: " + networkTopology.keySet());
            return;
        }

        ShortestPath shortestPathCalculator = new ShortestPath();
        List<String> path = shortestPathCalculator.computeShortestPath(networkTopology, source, destination);
        routingCache.addPath(source, destination, path);
        debug_print("Added path to cache: " + source + "->" + destination + ": " + path);
    }

    private List<String> ensurePathIsFound(String destination) {
        String source = getHostname() + ":" + getPort();
        ShortestPath shortestPathCalculator = new ShortestPath();
        List<String> path = shortestPathCalculator.computeShortestPath(networkTopology, source, destination);
        debug_print("Computed path: " + source + "->" + destination + ": " + path);
        return path != null ? path : new ArrayList<>();
    }

    private void sendMessageAlongPath(List<String> path, Random random) {
        String nextHopIdentifier = path.get(0);
        int payload = random.nextInt();
        Message message = new Message(new ArrayList<>(path.subList(1, path.size())), payload);
        for (int j = 0; j < 5; j++) {
            sendMessageToNextHop(nextHopIdentifier, message);
        }
    }

    private void sendTaskComplete() {
        TaskComplete taskComplete = new TaskComplete(getHostname(), getPort());
        try {
            sender.sendMessage(taskComplete.getBytes());
        } catch (IOException e) {
            debug_print("Error sending task complete message: " + e.getMessage());
        }
    }

    private void sendMessageToNextHop(String nextHopIdentifier, Message message) {
        debug_print("Sending message to next hop: " + nextHopIdentifier);
        String[] parts = nextHopIdentifier.split(":");
        String hostname = parts[0];
        int port = Integer.parseInt(parts[1]);

        sendTracker.incrementAndGet();
        sendSummation.addAndGet(message.getPayload());

        try (Socket socket = new Socket(hostname, port)) {
            TCPSender sender = new TCPSender(socket);
            sender.sendMessage(message.getBytes());

        } catch (IOException e) {
            debug_print("Failed to send message to " + nextHopIdentifier + ": " + e.getMessage());
        }
    }




    private synchronized void handleLinkWeights(LinkWeights event) {
        event.getLinkweights().forEach((link, weight) -> {
            String[] parts = link.split("@");
            String node1 = parts[0];
            String node2 = parts[1];
            Map<String, Integer> connections = networkTopology.getOrDefault(node1, new HashMap<>());
            connections.put(node2, weight);
            networkTopology.put(node1, connections);
        });
        System.out.println("Link weights are received and processed. Ready to send messages.");
    }



    // THis method is hanbdling the messaging nodes list by connecting to the nodes from the list per each node`
    private synchronized void handleMessagingNodesList(MessagingNodesList event) {
        List<String> messagingNodesInfo = event.getMessagingNodesInfo();

        if (messagingNodesInfo.isEmpty()) {
            debug_print("No peer messaging nodes to connect to.");
            return;
        }

        AtomicInteger connectionCount = new AtomicInteger(0);
        for (String nodeInfo : messagingNodesInfo) {
            String[] parts = nodeInfo.split(":");
            String hostname = parts[0];
            int port = Integer.parseInt(parts[1]);

            try {
                Socket socket = new Socket(hostname, port);
                new TCPReceiverThread(socket, this).start();
                connectionCount.incrementAndGet();
                debug_print("Connected to peer messaging node: " + nodeInfo);
            } catch (IOException e) {
                debug_print("Failed to connect to peer messaging node: " + nodeInfo);
            }
        }

        System.out.println(("All connections are established. Number of connections: " + connectionCount));
    }

    private synchronized void handleRegisterResponse(RegisterResponse response) {
        if (response.getStatusCode() == 1) {
            System.out.println(response.getAdditionalInfo());
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
        String source = normalizeHostnameToFQDN(getHostname()) + ":" + getPort();
        debug_print("Printing shortest paths from: " + source);

        ShortestPath shortestPathCalculator = new ShortestPath();
        networkTopology.keySet().forEach(destination -> {
            if (!destination.equals(source)) { // Avoid calculating path to itself
                List<String> path = shortestPathCalculator.computeShortestPath(networkTopology, source, destination);
                if (path != null && !path.isEmpty()) {
                    StringBuilder pathStr = new StringBuilder();
                    Iterator<String> pathIterator = path.iterator();
                    String prevNode = pathIterator.next();
                    pathStr.append(prevNode);

                    while (pathIterator.hasNext()) {
                        String currentNode = pathIterator.next();
                        Integer weight = networkTopology.get(prevNode).get(currentNode);
                        if (weight != null) {
                            pathStr.append("--").append(weight).append("--").append(currentNode);
                        } else {
                            debug_print("Error: Missing weight from " + prevNode + " to " + currentNode);
                            pathStr.append("--NULL--").append(currentNode);
                        }
                        prevNode = currentNode;
                    }

                    System.out.println(pathStr.toString());
                } else {
                    debug_print("No path found from " + source + " to " + destination);
                }
            }
        });
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
