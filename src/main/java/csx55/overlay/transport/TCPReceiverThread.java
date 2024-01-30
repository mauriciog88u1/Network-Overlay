package csx55.overlay.transport;

import csx55.overlay.node.Registry;
import csx55.overlay.util.DEBUG;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import csx55.overlay.node.Node;

import static csx55.overlay.util.DEBUG.debug_print;


public class TCPReceiverThread extends Thread {

    private Socket clientSocket;

    private Registry registry;

    private Node node;

    public TCPReceiverThread(Socket clientSocket, Registry registry) {
        this.clientSocket = clientSocket;
        this.registry = registry;
        debug_print("TCPReceiverThread created for client: " + clientSocket + " on thread: " + Thread.currentThread().getName());
    }

    @Override
    public void run() {
       debug_print("TCPReceiverThread started for client: " + clientSocket);
       debug_print("Listening for messages on ip: " + clientSocket.getInetAddress() + " port: " + clientSocket.getPort() + "");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                debug_print("Received message: " + inputLine);
                processInput(inputLine);
            }
        } catch (IOException e) {
            debug_print("Error in TCP Receiver: " + e.getMessage());
            System.out.println("Error in TCP Receiver: " + e.getMessage());
        } finally {
            closeSocket();
        }
    }

    private void processInput(String input) {
        if (input == null || input.isEmpty()) {
            debug_print("Received empty message.");
            return;
        }
        debug_print("Processed message: " + input);

        try {
            String[] nodeInfo = input.split(" ");
            String hostname, ip;
            int port;
            hostname = nodeInfo[0];
            ip = nodeInfo[1];
            port = Integer.parseInt(nodeInfo[2]);
            registry.registerNode(node);

        }
        catch (Exception e) {
            debug_print("Error in TCP Receiver: " + e.getMessage());

        }

    }

    private void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                debug_print("Socket closed for client: " + clientSocket);
            }
        } catch (IOException e) {
            debug_print("Error closing socket: " + e.getMessage());
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }
}
