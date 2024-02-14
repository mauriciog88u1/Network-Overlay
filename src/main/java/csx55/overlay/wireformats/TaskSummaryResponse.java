package csx55.overlay.wireformats;

import csx55.overlay.util.DEBUG;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TaskSummaryResponse implements Event{

    private int messageType;
    private String nodeIP;
    private int nodePort;
    private Long SummationOfSentMessages;
    private Long SummationOfReceivedMessages;
    private int relayedMessages;

    public TaskSummaryResponse(String nodeIP, int nodePort, long SummationOfSentMessages, long SummationOfReceivedMessages, int relayedMessages) {
        this.messageType = Protocol.TRAFFIC_SUMMARY;
        this.nodeIP = nodeIP;
        this.nodePort = nodePort;
        this.SummationOfSentMessages = SummationOfSentMessages;
        this.SummationOfReceivedMessages = SummationOfReceivedMessages;
        this.relayedMessages = relayedMessages;

    }

    public TaskSummaryResponse(byte[] data) {
        String[] dataArray = new String(data).split(" ");
        this.messageType = Integer.parseInt(dataArray[0]);
        this.nodeIP = dataArray[1];
        this.nodePort = Integer.parseInt(dataArray[2]);
        this.SummationOfSentMessages = Long.parseLong(dataArray[3]);
        this.SummationOfReceivedMessages = Long.parseLong(dataArray[4]);
        this.relayedMessages = Integer.parseInt(dataArray[5]);
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (baos; DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(messageType);
            dos.writeBytes(nodeIP + " ");
            dos.writeInt(nodePort);
            dos.writeLong(SummationOfSentMessages);
            dos.writeLong(SummationOfReceivedMessages);
            dos.writeInt(relayedMessages);
            dos.flush();
        } catch (IOException e) {
            DEBUG.debug_print("Error: TaskSummaryResponse: getBytes: " + e.getMessage());
        }
        return baos.toByteArray();

    }


    public String getNodeIP() {
        return nodeIP;
    }

    public int getNodePort() {
        return nodePort;
    }

    public Long getSummationOfSentMessages() {
        return SummationOfSentMessages;
    }

    public Long getSummationOfReceivedMessages() {
        return SummationOfReceivedMessages;
    }

    public int getRelayedMessages() {
        return relayedMessages;
    }

    @Override
    public int getType() {
        return messageType;
    }
}
