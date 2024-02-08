package csx55.overlay.wireformats;

import java.io.*;

public class DeregisterResponse implements Event {
    private final int messageType = Protocol.DEREGISTER_RESPONSE;
    private byte statusCode;
    private String additionalInfo;

    public DeregisterResponse(byte statusCode, String additionalInfo) {
        this.statusCode = statusCode;
        this.additionalInfo = additionalInfo;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(messageType);
        dos.writeByte(statusCode);
        byte[] infoBytes = additionalInfo.getBytes();
        dos.writeInt(infoBytes.length);
        dos.write(infoBytes);

        dos.flush();
        return baos.toByteArray();
    }

    @Override
    public int getType() {
        return messageType;
    }

    public byte getStatusCode() {
        return statusCode;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }
}
