package csx55.overlay.transport;

import csx55.overlay.util.DEBUG;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPReceiverThread extends Thread {

    private Socket clientSocket;

    public TCPReceiverThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        DEBUG.debug_print("TCPReceiverThread created for client: " + clientSocket);
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                DEBUG.debug_print("Received message: " + inputLine);
                processInput(inputLine);
            }
        } catch (IOException e) {
            DEBUG.debug_print("Error in TCP Receiver: " + e.getMessage());
            System.out.println("Error in TCP Receiver: " + e.getMessage());
        } finally {
            closeSocket();
        }
    }

    private void processInput(String input) {
        // Process the input received
        System.out.println("Received message: " + input);
    }

    private void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                DEBUG.debug_print("Socket closed for client: " + clientSocket);
            }
        } catch (IOException e) {
            DEBUG.debug_print("Error closing socket: " + e.getMessage());
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }
}
