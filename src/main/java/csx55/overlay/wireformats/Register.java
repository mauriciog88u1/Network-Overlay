package csx55.overlay.wireformats;

import java.io.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class Register implements Event {
    private final int messageType = Protocol.REGISTER_REQUEST;
    private final InetAddress ipAddress;
    private final int port;
//Register(InetAddress ipAddress, int port) is used for outgoing messages
    public Register(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }
//    Register(byte[] data) throws IOException  is used for incoming messages
   public Register(byte[] data) throws IOException {
    DataInputStream din = new DataInputStream(new ByteArrayInputStream(data));
    int messageType = din.readInt();
    if (messageType != Protocol.REGISTER_REQUEST) {
        throw new IllegalArgumentException("Incorrect message type for Register");
    }
    byte[] ipBytes = new byte[4];
    din.readFully(ipBytes);
    this.ipAddress = InetAddress.getByAddress(ipBytes);
    this.port = din.readInt();
}

    // Use static method for deserialization
    public static Register deserialize(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int messageType = buffer.getInt();
        if (messageType != Protocol.REGISTER_REQUEST) {
            throw new IllegalArgumentException("Incorrect message type for Register");
        }

        byte[] ipBytes = new byte[4]; // Assuming IPv4; adjust for IPv6 if needed
        buffer.get(ipBytes);
        InetAddress ipAddress = InetAddress.getByAddress(ipBytes);
        int port = buffer.getInt();

        return new Register(ipAddress, port);
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream daOutputStream = new DataOutputStream(baOutputStream);

        daOutputStream.writeInt(messageType);
        byte[] ipBytes = ipAddress.getAddress();
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
}
