package csx55.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Register implements Event {
    private final int messageType;
    private final String ipAddress;
    private final int port;

   public Register(byte[] data){
       deserialize(data);
   }
   public Register(String ipAddress, int port) {
       this.messageType = Protocol.REGISTER_REQUEST;
       this.ipAddress = ipAddress;
       this.port = port;
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream daOutputStream = new DataOutputStream(baOutputStream);

        daOutputStream.writeInt(messageType);
        daOutputStream.writeInt(ipAddress.length());
        daOutputStream.writeBytes(ipAddress);
        daOutputStream.writeInt(port);

        daOutputStream.flush();
        byte[] marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        daOutputStream.close();

        return marshalledBytes;
    }

    @Override
    public int getType() {
        return messageType;
    }


    public Register deserialize(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int messageType = buffer.getInt();
        if (messageType != Protocol.REGISTER_REQUEST) {
            throw new IllegalArgumentException("Incorrect message type for Register");
        }

        int ipLength = buffer.getInt();
        byte[] ipBytes = new byte[ipLength];
        buffer.get(ipBytes);
        String ipAddress = new String(ipBytes);

        int port = buffer.getInt();

        return new Register(ipAddress, port);
    }
}
