package csx55.overlay.transport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;

public class TCPSender {

    private Socket clientSocket;
    private BufferedWriter out;



    public TCPSender(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e) {
            System.out.println("Error initializing output stream: " + e.getMessage());
        }
    }

    public void sendMessage(byte[] message) {
    try {
        out.write(Arrays.toString(message));
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
