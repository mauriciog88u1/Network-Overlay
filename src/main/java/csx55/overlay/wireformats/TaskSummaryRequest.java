package csx55.overlay.wireformats;

import java.io.*;

public class TaskSummaryRequest implements Event {

    private final int messageType ;

    public TaskSummaryRequest() {
        this.messageType = Protocol.PULL_TRAFFIC_SUMMARY;
    }
    public TaskSummaryRequest(byte[] data) throws IOException {
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(data));
        this.messageType = din.readInt();

    }


    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(messageType);
        dos.flush();
        return baos.toByteArray();
    }

    @Override
    public int getType() {
        return messageType;
    }
}
