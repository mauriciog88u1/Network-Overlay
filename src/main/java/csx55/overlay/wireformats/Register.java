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
        if (messageType != Protocol.REGISTER_REQUEST || ipAddress == null) {
            throw new IllegalArgumentException("Incorrect message type for Register or ipAddress is null");
        }

        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(messageType);

        byte[] ipAddressBytes = ipAddress.getBytes();
        dout.writeInt(ipAddressBytes.length);
        dout.write(ipAddressBytes);

        dout.writeInt(port);

        dout.flush();
        byte[] marshalledBytes = baOutputStream.toByteArray();

        dout.close();
        baOutputStream.close();

        return marshalledBytes;
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
}
