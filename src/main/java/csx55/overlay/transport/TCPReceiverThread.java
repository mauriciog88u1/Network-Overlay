package csx55.overlay.transport;

import csx55.overlay.node.Node;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import static csx55.overlay.util.DEBUG.DEBUG;
import static csx55.overlay.util.DEBUG.debug_print;
import static csx55.overlay.wireformats.EventFactory.createEvent;

public class TCPReceiverThread extends Thread {

    private Socket clientSocket;
    private Node node;

    public TCPReceiverThread(Socket clientSocket, Node node) {
        this.clientSocket = clientSocket;
        this.node = node;
        debug_print("TCPReceiverThread created for client: " + clientSocket + " on thread: " + Thread.currentThread().getName());
    }

    @Override
    public void run() {
        debug_print("TCPReceiverThread started for client: " + clientSocket);
        debug_print("Listening for messages on ip: " + clientSocket.getInetAddress() + " port: " + clientSocket.getPort() + "");
        try {
            DataInputStream din = new DataInputStream(clientSocket.getInputStream());
            while (!clientSocket.isClosed()) {
                int dataLength = din.readInt();
                if(dataLength > 0) {
                    byte[] data = new byte[dataLength];
                    din.readFully(data, 0, dataLength);
                    debug_print("Received " + dataLength + " bytes from " + clientSocket + ": " + bytesToHex(data));
                    node.onEvent(createEvent(data));
                }
            }
        } catch (IOException e) {
            debug_print("Error in TCP Receiver: " + e.getMessage());
        } finally {
            closeSocket();
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
        }
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex).append(' ');
        }
        return hexString.toString();
    }

}
