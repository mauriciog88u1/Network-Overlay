package csx55.overlay.wireformats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegisterResponse implements Event {
    private final int messageType = Protocol.REGISTER_RESPONSE;
    private final byte statusCode;
    private final String additionalInfo;

    // Constructor for deserializing data
    public RegisterResponse(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(baInputStream);

        din.readInt();
        this.statusCode = din.readByte();
        this.additionalInfo = readString(din);

        din.close();
        baInputStream.close();
    }

    public RegisterResponse(byte statusCode, String additionalInfo) {
        this.statusCode = statusCode;
        this.additionalInfo = additionalInfo;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(baOutputStream);

        dout.writeInt(messageType);
        dout.writeByte(statusCode);
        writeString(dout, additionalInfo);

        dout.flush();
        byte[] marshalledBytes = baOutputStream.toByteArray();

        baOutputStream.close();
        dout.close();

        return marshalledBytes;
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

    private void writeString(DataOutputStream dout, String s) throws IOException {
        byte[] bytes = s.getBytes();
        dout.writeInt(bytes.length);
        dout.write(bytes);
    }

    private String readString(DataInputStream din) throws IOException {
        int length = din.readInt();
        byte[] bytes = new byte[length];
        din.readFully(bytes);
        return new String(bytes);
    }
}
