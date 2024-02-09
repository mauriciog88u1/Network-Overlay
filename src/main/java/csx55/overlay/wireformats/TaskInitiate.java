package csx55.overlay.wireformats;

import java.io.*;

public class TaskInitiate implements Event {
    private int messageType = Protocol.TASK_INITIATE;
    private int rounds;

    // Constructor for creating an instance to send
    public TaskInitiate(int rounds) {
        this.rounds = rounds;
    }

    // Constructor for deserializing data
    public TaskInitiate(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        this.messageType = dis.readInt(); // Assuming the first read is the message type
        this.rounds = dis.readInt();
    }

    @Override
    public int getType() {
        return messageType;
    }

    public int getRounds() {
        return rounds;
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(messageType);
        dos.writeInt(rounds);

        dos.flush();
        return baos.toByteArray();
    }
}
