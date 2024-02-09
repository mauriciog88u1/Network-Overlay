package csx55.overlay.wireformats;

import java.io.DataInputStream;
import java.io.IOException;

public class TaskComplete implements Event {
    private final int messageType = Protocol.TASK_COMPLETE;


    public TaskComplete(byte[] data) {
        DataInputStream din = new DataInputStream(new java.io.ByteArrayInputStream(data));
        try {
            int messageType = din.readInt();
            if (messageType != Protocol.TASK_COMPLETE) {
                throw new IllegalArgumentException("Incorrect message type for TaskComplete");
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
    @Override
    public byte[] getBytes() throws IOException {
        return new byte[0];

    }

    @Override
    public int getType() {
        return messageType;
    }

}
