package csx55.overlay.node;

import csx55.overlay.transport.TCPServerThread;
import csx55.overlay.util.OverlayCreator;
import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.Event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

public class Registry implements Node{
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
    }

    private void listMessagingNodes() {
       for (String node : registeredNodes.keySet()) {
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

    public void registerNode(String hostname, String ip, int port) {
        DEBUG.debug_print("Registering node: " + hostname + " " + ip + " " + port);
        registeredNodes.put(hostname,null);
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == -1) {
            DEBUG.debug_print("Error in event type.");
        }


    }

    @Override
    public String getHostname() {
        return "";
    }

    @Override
    public String getIp() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }


}