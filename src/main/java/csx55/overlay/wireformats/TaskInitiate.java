package csx55.overlay.wireformats;

import java.io.*;

public class TaskInitiate implements Event {
    private int messageType = Protocol.TASK_INITIATE;
    private int rounds;

    public TaskInitiate(int rounds) {
        this.rounds = rounds;
    }

    public TaskInitiate(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        this.messageType = dis.readInt();
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
