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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static csx55.overlay.util.DEBUG.debug_print;

public class MessagingNode implements Node {
    private TCPSender sender;
    private ServerSocket serverSocket;

    private final AtomicInteger sendTracker = new AtomicInteger();
    private final AtomicInteger receiveTracker = new AtomicInteger();

    private Map<String, Map<String, Integer>> networkTopology;

    private RoutingCache routingCache;



    public MessagingNode(String registryHost, int registryPort) {
        try {
            Socket registrySocket = new Socket(registryHost, registryPort);
            this.sender = new TCPSender(registrySocket);
            this.serverSocket = new ServerSocket(0); // Dynamically allocate a port
            this.routingCache = new RoutingCache();
            this.networkTopology = new HashMap<>();


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
        else if (event instanceof Message) {
            handleReceivedMessage((Message) event);
        }
    }

    private void handleReceivedMessage(Message event) {
        receiveTracker.incrementAndGet();
        if (event.getRroutingTable().size() > 1) {
            String nextHopIdentifier = event.getRroutingTable().get(0);
            event.getRroutingTable().remove(0);
            sendMessageToNextHop(nextHopIdentifier, event);
        } else {
            debug_print("Received message: " + event.getPayload());
        }
    }

    private void handleTaskInitiate(TaskInitiate event) {
        DEBUG.debug_print("Received task initiate event: " + event.getRounds());

        Random random = new Random();

        for (int i = 0; i < event.getRounds(); i++) {
            String destination = networkTopology.keySet().stream().skip(random.nextInt(networkTopology.size())).findFirst().orElse(null);
            List<String> path = routingCache.getPath(this.getIp() + ":" + this.getPort(), destination);
            DEBUG.debug_print("Path to " + destination + ": " + path);

            if (path == null) {
                DEBUG.debug_print("Path not found in cache. Computing and caching...");
                computeAndCacheShortestPath(destination);
                path = routingCache.getPath(this.getIp() + ":" + this.getPort(), destination);
            }
            if (path != null && !path.isEmpty()) {
                DEBUG.debug_print("Sending message to next hop...");
                path.remove(0);
                if (!path.isEmpty()) {
                    DEBUG.debug_print("path not emptty next hop is: " + path.get(0) + " sending message" );
                    String nextHopIdentifier = path.get(0);
                    int payload = random.nextInt();
                    Message message = new Message(path, payload);
                    debug_print("Sending message to next hop: " + nextHopIdentifier);
                    sendMessageToNextHop(nextHopIdentifier, message);
                    sendTracker.incrementAndGet();
                }
            }
        }
    }

    private void sendMessageToNextHop(String nextHopIdentifier, Message message) {
        String[] parts = nextHopIdentifier.split(":");
        String hostname = parts[0];
        int port = Integer.parseInt(parts[1]);

        try {
            Socket socket = new Socket(hostname, port);
            TCPSender sender = new TCPSender(socket);
            sender.sendMessage(message.getBytes());
        } catch (IOException e) {
            debug_print("Failed to send message to " + nextHopIdentifier + ": " + e.getMessage());
        }
    }



    private void handleLinkWeights(LinkWeights event) {
        debug_print("Received link weights from registry:");
        System.out.println("Link weights received and processed. Ready to send messages.");

        event.getLinkweights().forEach((link, weight) -> {
            String[] nodes = link.split("-");
            String source = nodes[0];
            String destination = nodes[1];

            networkTopology.putIfAbsent(source, new HashMap<>());
            networkTopology.get(source).put(destination, weight);
        });
    }
    public void computeAndCacheShortestPath(String destination) {
        String source = this.getIp() + ":" + this.getPort();
        if (!networkTopology.containsKey(source)) {
            debug_print("No paths from this node.");
            return;
        }

        ShortestPath shortestPathCalculator = new ShortestPath();
        List<String> path = shortestPathCalculator.computeShortestPath(networkTopology, source, destination);
        routingCache.addPath(source, destination, path);
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
        routingCache.printCache();
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
