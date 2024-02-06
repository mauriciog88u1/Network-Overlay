package csx55.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Deregister implements Event {
    private final int messageType = Protocol.DEREGISTER_REQUEST;
    private final String ipAddress;
    private final int port;

    public Deregister(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
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
}
