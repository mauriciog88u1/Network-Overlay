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

import static csx55.overlay.util.DEBUG.debug_print;
import static csx55.overlay.wireformats.Protocol.*;

public class MessagingNode implements Node {
    private TCPSender sender;
    private ServerSocket serverSocket;
    private final AtomicInteger sendTracker = new AtomicInteger();
    private final AtomicInteger receiveTracker = new AtomicInteger();
    private ConcurrentHashMap<String, Map<String, Integer>> networkTopology;
    private RoutingCache routingCache;
    private String localNodeIdentifier;


    public MessagingNode(String registryHost, int registryPort) {
        try {
            serverSocket = new ServerSocket(0); // Dynamically allocate a port
            localNodeIdentifier = InetAddress.getLocalHost().getCanonicalHostName() + ":" + serverSocket.getLocalPort();
            Socket registrySocket = new Socket(registryHost, registryPort);
            sender = new TCPSender(registrySocket);
            networkTopology = new ConcurrentHashMap<>();

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
        String command;
        try {
            while ((command = reader.readLine()) != null) {
                processCommand(command);
            }
        } catch (IOException e) {
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
        Register register = new Register(localHost.getCanonicalHostName(), localHost.getHostAddress(), serverSocket.getLocalPort());
        sender.sendMessage(register.getBytes());
        debug_print("Registration message sent to registry.");
    }

    private void deregisterFromRegistry() {
        try {
            Deregister deregister = new Deregister(getIp(), getPort());
            sender.sendMessage(deregister.getBytes());
            System.out.println("Deregistered from registry.");
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
        debug_print("Received event: " + event.getType());
        switch (event.getType()) {
            case REGISTER_RESPONSE:
                handleRegisterResponse((RegisterResponse) event);
                break;
            case MESSAGING_NODES_LIST:
                handleMessagingNodesList((MessagingNodesList) event);
                break;
            case LINK_WEIGHTS:
                handleLinkWeights((LinkWeights) event);
                break;
            case TASK_INITIATE:
                handleTaskInitiate((TaskInitiate) event);
                break;
            case MESSAGE:
                handleReceivedMessage((Message) event);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event.getType());
        }
    }

    private void handleReceivedMessage(Message event) {
        receiveTracker.incrementAndGet();
        String nextHopIdentifier = getNextHop(event);
        if (!nextHopIdentifier.isEmpty()) {
            sendMessageToNextHop(nextHopIdentifier, event);
        }
    }

    private String getNextHop(Message event) {
        if (event.getRroutingTable().size() > 1) {
            return event.getRroutingTable().get(0);
        }
        return "";
    }

    private void handleTaskInitiate(TaskInitiate event) {
        debug_print("Handling task initiate with " + event.getRounds() + " rounds.");
        debug_print("network topology: " + networkTopology.keySet());
//        make sure link weights are received before sending messages
        if (networkTopology.isEmpty()) {
            debug_print("No link weights received yet. Waiting...");
            return;
        }

        Random random = new Random();
        for (int i = 0; i < event.getRounds(); i++) {
            String destination = getRandomDestination(networkTopology);
            if (!destination.equals(getSelfIdentifier()) && !destination.isEmpty()) {
                List<String> path = routingCache.getPath(getSelfIdentifier(), destination);
                if (path == null) {
                    debug_print("Path to " + destination + " not found in cache. Computing...");
                    computeAndCacheShortestPath(destination);
                    path = routingCache.getPath(getSelfIdentifier(), destination);
                }
                if (path != null && !path.isEmpty()) {
                    String nextHopIdentifier = path.get(0);
                    debug_print("Sending message to " + nextHopIdentifier + " with path: " + path);
                    sendMessageToNextHop(nextHopIdentifier, new Message(path, random.nextInt()));
                } else {
                    debug_print("No path found to " + destination);
                }
            } else {
                debug_print("Invalid destination: " + destination);
            }
        }
    }

    private String getRandomDestination(ConcurrentHashMap<String, Map<String, Integer>> networktopology) {
        List<String> nodes = new ArrayList<>(networktopology.keySet());
        DEBUG.debug_print("Nodes: " + nodes);

        return nodes.get(new Random().nextInt(nodes.size()));

    }

    private String getSelfIdentifier() {
        try {
            String selfIdentifier = InetAddress.getLocalHost().getCanonicalHostName() + ":" + getPort();
            debug_print("Self identifier: " + selfIdentifier);
            return selfIdentifier;
        } catch (UnknownHostException e) {
            debug_print("Unable to determine self identifier.");
            return "unknown";
        }
    }


    private void sendMessageToNextHop(String nextHopIdentifier, Message message) {
        try {

            String[] parts = nextHopIdentifier.split(":");
            Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
            debug_print("Sending message to next hop: " + nextHopIdentifier);
            new TCPSender(socket).sendMessage(message.getBytes());
        } catch (IOException e) {
            debug_print("Error sending message to next hop: " + e.getMessage());
        }
    }

    private void handleLinkWeights(LinkWeights event) {

        event.getLinkweights().forEach((link, weight) -> {
            String[] nodes = link.split("-");

            try{
                if (nodes[0].equals(getSelfIdentifier())) {
                    debug_print("Adding link weight: " + nodes[0] + " -> " + nodes[1] + " : " + weight);
                    networkTopology.computeIfAbsent(nodes[0], k -> new HashMap<>()).put(nodes[1], weight);
                } else if (nodes[1].equals(getSelfIdentifier())) {
                    debug_print("Adding link weight: " + nodes[1] + " -> " + nodes[0] + " : " + weight);
                    networkTopology.computeIfAbsent(nodes[1], k -> new HashMap<>()).put(nodes[0], weight);
                }
            } catch (Exception e) {
                debug_print("Error adding link weight: " + e.getMessage());
            }
            if (networkTopology.size() < 0) {
                debug_print("network topology is empty");
                
            }
        });
    }

    public void computeAndCacheShortestPath(String destination) {
        if (!networkTopology.containsKey(getSelfIdentifier())) {
            debug_print("destination is " + destination + " and self is " + getSelfIdentifier());
            return;
        }
        List<String> path = new ShortestPath().computeShortestPath(networkTopology, getSelfIdentifier(), destination);
        debug_print("Computed shortest path to " + destination + ": " + path);
        routingCache.addPath(getSelfIdentifier(), destination, path);
    }

    private void handleMessagingNodesList(MessagingNodesList event) {
        List<String> messagingNodesInfo = event.getMessagingNodesInfo();

        if (messagingNodesInfo.isEmpty()) {
            debug_print("No peer messaging nodes to connect to.");
            return;
        }

        AtomicInteger connectionCount = new AtomicInteger();
        for (String nodeInfo : messagingNodesInfo) {
            String[] parts = nodeInfo.split(":");
            String hostname = parts[0];
            int port = Integer.parseInt(parts[1]);
            try {
                // Establish connection to each specified node
                Socket socket = new Socket(hostname, port);
                // Start listening for incoming messages on this connection
                new TCPReceiverThread(socket, this).start();
                connectionCount.incrementAndGet();
                debug_print("Connected to peer messaging node: " + nodeInfo);
            } catch (IOException e) {
                debug_print("Error connecting to node: " + nodeInfo + " - " + e.getMessage());
            }
        }
        debug_print("All connections are established. Number of connections: " + connectionCount.get());
    }

    private void handleRegisterResponse(RegisterResponse response) {
         if (response.getStatusCode() == 1) {
                System.out.println("Successfully registered with registry");

          } else {
             System.out.println("Failed to register with registry");
                System.exit(1);
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
                routingCache.printCache();
                break;
            case "exit-overlay":
                deregisterFromRegistry();
                System.exit(0);
                break;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Usage: java MessagingNode <registry host> <registry port> --DEBUG ");
            return;
        }
        if (args.length == 3 && args[2].equals("--DEBUG")) {
            DEBUG.DEBUG = true;
        }
        debug_print("Starting MessagingNode with registry host: " + args[0] + " registry port: " + args[1] );
        new MessagingNode(args[0], Integer.parseInt(args[1]));
    }
}
