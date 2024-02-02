package csx55.overlay.node;

import csx55.overlay.transport.TCPSender;
import csx55.overlay.wireformats.Register;

import java.net.InetAddress;
import java.net.Socket;

import static csx55.overlay.util.DEBUG.debug_print;


public class MessagingNode {
    private int dynmaicPort = 1025;
    private TCPSender sender;
    private Socket socket;


    public MessagingNode(String registryHost, int registryPort) {
        debug_print("MessagingNode created: " + registryHost + " " + registryPort);


    }

    public static void main(String[] args) {
        String registryHost = args[0];
        int registryPort = Integer.parseInt(args[1]);
        MessagingNode messagingNode = new MessagingNode(registryHost, registryPort);
        messagingNode.start();

    }

    private void start() {
        debug_print("MessagingNode started.");
        connectToRegistry();
        registerWithRegistry();

    }

public void connectToRegistry() {
    try {
         socket = new Socket(InetAddress.getLocalHost(), dynmaicPort);
        sender = new TCPSender(socket);
        debug_print("Connected to registry on port: " + dynmaicPort);
    } catch (Exception e) {
        debug_print("Error connecting to registry: " + e.getMessage());
        dynmaicPort++;
        connectToRegistry();
    }
}

public void registerWithRegistry() {
    try {
        Register register = new Register(socket.getInetAddress(), socket.getPort());
        sender.sendMessage(register.getBytes());
        debug_print("Registration message sent to registry.");
    } catch (Exception e) {
        debug_print("Error registering with registry: " + e.getMessage());
    }
}
}
