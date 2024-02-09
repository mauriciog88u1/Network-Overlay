package csx55.overlay.transport;

import csx55.overlay.util.DEBUG;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class TCPSender {

    private Socket clientSocket;
    private DataOutputStream out;

    public TCPSender(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error initializing output stream: " + e.getMessage());
        }
    }

    public void sendMessage(byte[] message) {
        try {
            out.writeInt(message.length);
            out.write(message);
//            DEBUG.debug_print("Sent message: " + bytesToHex(message)); This provides bytes useful for debuggin but leaving commented because it spams cli
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (out != null) {
                out.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

}

