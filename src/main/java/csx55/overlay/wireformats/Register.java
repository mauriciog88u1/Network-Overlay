package csx55.overlay.wireformats;

import java.io.*;

public class Register implements Event {
    private final int messageType = Protocol.REGISTER_REQUEST;

    private final String ipAddress;
    private final int port;

    public Register(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public Register(byte[] data) throws IOException {
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(data));
        int messageType = din.readInt();
        if (messageType != Protocol.REGISTER_REQUEST) {
            throw new IllegalArgumentException("Incorrect message type for Register");
        }
        int ipLength = din.readInt();
        byte[] ipBytes = new byte[ipLength];
        din.readFully(ipBytes);
        this.ipAddress = new String(ipBytes);
        this.port = din.readInt();
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream daOutputStream = new DataOutputStream(baOutputStream);

        daOutputStream.writeInt(messageType);
        byte[] ipBytes = ipAddress.getBytes();
        daOutputStream.writeInt(ipBytes.length);
        daOutputStream.write(ipBytes);
        daOutputStream.writeInt(port);

        daOutputStream.flush();
        return baOutputStream.toByteArray();
    }

    @Override
    public int getType() {
        return messageType;
    }

    // Additional getter methods for ipAddress and port, if needed
    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
}
