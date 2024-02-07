package csx55.overlay.transport;

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
}
