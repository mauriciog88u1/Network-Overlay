package csx55.overlay.wireformats;

import java.io.*;
import java.net.InetAddress;

public class Deregister implements Event {
    private final int messageType = Protocol.DEREGISTER_REQUEST;
    private final String ipAddress;
    private final int port;

    private String hostname;


    public Deregister(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public Deregister(byte[] data) throws IOException {
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(data));
        int messageType = din.readInt();
        if (messageType != Protocol.DEREGISTER_REQUEST) {
            throw new IllegalArgumentException("Incorrect message type for Deregister");
        }
        try {
            int ipLength = din.readInt();
            byte[] ipBytes = new byte[ipLength];
            din.readFully(ipBytes);
            this.ipAddress = new String(ipBytes);
            this.port = din.readInt();
            this.hostname = InetAddress.getByName(ipAddress).getHostName();
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading from byte array: " + e.getMessage());
        }



    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(messageType);
        byte[] ipBytes = ipAddress.getBytes();
        dos.writeInt(ipBytes.length);
        dos.write(ipBytes);
        dos.writeInt(port);

        dos.flush();
        return baos.toByteArray();
    }

    @Override
    public int getType() {
        return messageType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }
}
