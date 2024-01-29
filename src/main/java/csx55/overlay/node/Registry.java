package csx55.overlay.node;

import csx55.overlay.transport.TCPServerThread;
import csx55.overlay.util.OverlayCreator;
import csx55.overlay.util.DEBUG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Registry {
    private final TCPServerThread serverThread;
    private final HashMap<String, Node> registeredNodes;
    private final OverlayCreator overlayCreator;

    public Registry(int serverPort) {
        DEBUG.debug_print("Initializing Registry on port: " + serverPort);
        serverThread = new TCPServerThread(serverPort, this);
        registeredNodes = new HashMap<>();
        overlayCreator = new OverlayCreator();
    }


    public void start() {
        DEBUG.debug_print("Starting Registry server thread.");
        serverThread.start();
        handleCommands();
    }


    private void handleCommands() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String command;
        try {
            while ((command = reader.readLine()) != null) {
                DEBUG.debug_print("Received command: " + command);
                switch (command.split(" ")[0]) {
                    case "list-messaging-nodes":
                        listMessagingNodes();
                        break;
                    case "list-weights":
                        listWeights();
                        break;
                    case "setup-overlay":
                        int numberOfConnections = Integer.parseInt(command.split(" ")[1]);
                        setupOverlay(numberOfConnections);
                        break;
                    case "send-overlay-link-weights":
                        sendOverlayLinkWeights();
                        break;
                    default:
                        String usage = "Usage:\n" +
                                       "list-messaging-nodes\n" +
                                       "list-weights\n" +
                                       "setup-overlay <number-of-connections>\n" +
                                       "send-overlay-link-weights\n";
                        System.out.println(usage);
                }
            }
        } catch (IOException e) {
            DEBUG.debug_print("Error handling command: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void listMessagingNodes() {
        for (Node node : registeredNodes.values()) {
            System.out.println(node);
        }
    }

    private void listWeights() {
        // TODO document why this method is empty
    }

    private void setupOverlay(int numberOfConnections) {
        overlayCreator.createOverlay(numberOfConnections, registeredNodes);
    }

    private void sendOverlayLinkWeights() {
        // TODO document why this method is empty
    }
    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2) {
            System.out.println("Usage: java csx55.overlay.node.Registry <port-number> --DEBUG");
            return;
        }
        if (args.length == 2 && (args[1].equals("--DEBUG"))) {
                DEBUG.DEBUG = true;
        }

        int port = Integer.parseInt(args[0]);
        Registry registry = new Registry(port);
        registry.start();
    }

    public void deregisterNode() {


    }

    public void registerNode(Node node) {
        DEBUG.debug_print("Registering node: " + node);
        registeredNodes.put(node.getHostname(), node);
    }
}