package csx55.overlay.wireformats;

import java.io.*;

public class TaskSummaryResponse implements Event {

    private int messageType;
    private String nodeIP;
    private int nodePort;
    private long summationOfSentMessages;
    private long summationOfReceivedMessages;
    private int relayedMessages;

    public TaskSummaryResponse(String nodeIP, int nodePort, long summationOfSentMessages, long summationOfReceivedMessages, int relayedMessages) {
        this.messageType = Protocol.TRAFFIC_SUMMARY;
        this.nodeIP = nodeIP;
        this.nodePort = nodePort;
        this.summationOfSentMessages = summationOfSentMessages;
        this.summationOfReceivedMessages = summationOfReceivedMessages;
        this.relayedMessages = relayedMessages;
    }

    public TaskSummaryResponse(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        this.messageType = dis.readInt();
        this.nodeIP = dis.readUTF();
        this.nodePort = dis.readInt();
        this.summationOfSentMessages = dis.readLong();
        this.summationOfReceivedMessages = dis.readLong();
        this.relayedMessages = dis.readInt();
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(messageType);
        dos.writeUTF(nodeIP);
        dos.writeInt(nodePort);
        dos.writeLong(summationOfSentMessages);
        dos.writeLong(summationOfReceivedMessages);
        dos.writeInt(relayedMessages);

        dos.flush();
        return baos.toByteArray();
    }

    public String getNodeIP() {
        return nodeIP;
    }

    public int getNodePort() {
        return nodePort;
    }

    public long getSummationOfSentMessages() {
        return summationOfSentMessages;
    }

    public long getSummationOfReceivedMessages() {
        return summationOfReceivedMessages;
    }

    public int getRelayedMessages() {
        return relayedMessages;
    }

    @Override
    public int getType() {
        return messageType;
    }
}
