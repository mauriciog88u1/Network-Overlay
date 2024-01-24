package csx55.overlay.transport;

import csx55.overlay.node.Registry;
import csx55.overlay.util.DEBUG;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServerThread extends Thread {

    private int serverPort;
    private Registry registry;
    private ServerSocket serverSocket;
    private boolean isRunning;

    public TCPServerThread(int serverPort, Registry registry) {
        this.serverPort = serverPort;
        this.registry = registry;
        this.isRunning = false;
        try {
            this.serverSocket = new ServerSocket(serverPort);
            DEBUG.debug_print("Server socket created on port: " + serverPort + " ip: " + serverSocket.getInetAddress());
        } catch (IOException e) {
            DEBUG.debug_print("Error creating server socket: " + e.getMessage());
            System.out.println("Error creating server socket: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        DEBUG.debug_print("Server thread started.");
        this.isRunning = true;
        try {
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                DEBUG.debug_print("Client connection accepted: " + clientSocket);

                TCPReceiverThread clientThread = new TCPReceiverThread(clientSocket);
                clientThread.start();
            }
        } catch (IOException e) {
            if (isRunning) {
                DEBUG.debug_print("Server thread interrupted: " + e.getMessage());
            }
        } finally {
            stopServer();
        }
    }

    public void stopServer() {
        this.isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                DEBUG.debug_print("Server socket closed.");
            }
        } catch (IOException e) {
            DEBUG.debug_print("Error closing server socket: " + e.getMessage());
            System.out.println("Error closing server socket: " + e.getMessage());
        }
    }
}
