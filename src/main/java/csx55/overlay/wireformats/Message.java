package csx55.overlay.wireformats;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Message implements Event {

    private final int messageType = Protocol.MESSAGE;
    private final List<String> routingTable;
    private final int payload;

    public Message(List<String> routingTable, int payload) {
        this.routingTable = routingTable;
        this.payload = payload;
    }

    public Message(byte[] messageBytes) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(messageBytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));

        int type = din.readInt();
        if (type != messageType) {
            throw new IllegalArgumentException("Incorrect message type");
        }

        this.payload = din.readInt();

        int routingPlanSize = din.readInt();
        this.routingTable = new ArrayList<>();
        for (int i = 0; i < routingPlanSize; i++) {
            this.routingTable.add(din.readUTF());
        }

        baInputStream.close();
        din.close();
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);
        dout.writeInt(this.payload);

        dout.writeInt(this.routingTable.size());
        for (String node : this.routingTable) {
            dout.writeUTF(node);
        }

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

    public int getPayload() {
        return payload;
    }

    public List<String> getRroutingTable() {
        return routingTable;
    }
}
