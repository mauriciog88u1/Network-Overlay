package csx55.overlay.node;

import csx55.overlay.dijkstra.RoutingCache;
import csx55.overlay.dijkstra.ShortestPath;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.*;
import csx55.overlay.wireformats.Protocol.*;

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

import static csx55.overlay.wireformats.Protocol.*;

public class MessagingNode implements Node {
    private TCPSender sender;
    private ServerSocket serverSocket;
    private final AtomicInteger sendTracker = new AtomicInteger();
    private final AtomicInteger receiveTracker = new AtomicInteger();
    private ConcurrentHashMap<String, Map<String, Integer>> networkTopology;
    private RoutingCache routingCache;

    public MessagingNode(String registryHost, int registryPort) {
        try {
            Socket registrySocket = new Socket(registryHost, registryPort);
            this.sender = new TCPSender(registrySocket);
            this.serverSocket = new ServerSocket(0);
            this.routingCache = new RoutingCache();
            this.networkTopology = new ConcurrentHashMap<>();
            listenForConnections();
            registrationToRegistry();
            listenForCommands();
            setupShutdownHook();
        } catch (IOException e) {
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
                    new TCPReceiverThread(clientSocket, this).start();
                }
            } catch (IOException e) {
            }
        }).start();
    }

    private void registrationToRegistry() throws IOException {
        InetAddress localHost = InetAddress.getLocalHost();
        Register register = new Register(localHost.getHostName(), localHost.getHostAddress(), serverSocket.getLocalPort());
        sender.sendMessage(register.getBytes());
    }

    private void deregisterFromRegistry() {
        try {
            Deregister deregister = new Deregister(getIp(), getPort());
            sender.sendMessage(deregister.getBytes());
        } catch (IOException e) {
        }
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::deregisterFromRegistry));
    }

    @Override
    public void onEvent(Event event) {
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
        Random random = new Random();
        for (int i = 0; i < event.getRounds(); i++) {
            String destination = getRandomDestination();
            if (!destination.isEmpty()) {
                List<String> path = routingCache.getPath(getSelfIdentifier(), destination);
                if (path == null) {
                    computeAndCacheShortestPath(destination);
                    path = routingCache.getPath(getSelfIdentifier(), destination);
                }
                if (path != null && !path.isEmpty()) {
                    String nextHopIdentifier = path.get(0);
                    sendMessageToNextHop(nextHopIdentifier, new Message(path, random.nextInt()));
                }
            }
        }
    }

    private String getRandomDestination() {
        List<String> keys = new ArrayList<>(networkTopology.keySet());
        keys.remove(getSelfIdentifier());
        if (keys.isEmpty()) return "";
        return keys.get(new Random().nextInt(keys.size()));
    }

    private String getSelfIdentifier() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName() + ":" + getPort();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    private void sendMessageToNextHop(String nextHopIdentifier, Message message) {
        try {
            String[] parts = nextHopIdentifier.split(":");
            Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
            new TCPSender(socket).sendMessage(message.getBytes());
        } catch (IOException e) {
        }
    }

    private void handleLinkWeights(LinkWeights event) {
        event.getLinkweights().forEach((link, weight) -> {
            String[] nodes = link.split("-");
            networkTopology.computeIfAbsent(nodes[0], k -> new HashMap<>()).put(nodes[1], weight);
        });
    }

    public void computeAndCacheShortestPath(String destination) {
        if (!networkTopology.containsKey(getSelfIdentifier())) {
            return;
        }
        List<String> path = new ShortestPath().computeShortestPath(networkTopology, getSelfIdentifier(), destination);
        routingCache.addPath(getSelfIdentifier(), destination, path);
    }

    private void handleMessagingNodesList(MessagingNodesList event) {
        event.getMessagingNodesInfo().forEach(nodeInfo -> {
            String[] parts = nodeInfo.split(":");
            try {
                new TCPReceiverThread(new Socket(parts[0], Integer.parseInt(parts[1])), this).start();
            } catch (IOException e) {
            }
        });
    }

    private void handleRegisterResponse(RegisterResponse response) {
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
        if (args.length != 2) {
            System.out.println("Usage: java MessagingNode <registry host> <registry port>");
            return;
        }

        new MessagingNode(args[0], Integer.parseInt(args[1]));
    }
}
