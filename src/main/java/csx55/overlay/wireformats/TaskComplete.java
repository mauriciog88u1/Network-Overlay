package csx55.overlay.wireformats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TaskComplete implements Event {
    private int messageType;
    private String nodeIPAddress; 
    private int nodePort; 

    public TaskComplete(String nodeIPAddress, int nodePort) {
        this.nodeIPAddress = nodeIPAddress;
        this.nodePort = nodePort;
    }

    public TaskComplete(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        this.messageType = dis.readInt(); 
        this.nodeIPAddress = dis.readUTF(); 
        this.nodePort = dis.readInt(); 
    }

    @Override
    public int getType() {
        return messageType;
    }

    public String getNodeIPAddress() {
        return nodeIPAddress;
    }

    public int getNodePort() {
        return nodePort;
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        this.messageType = Protocol.TASK_COMPLETE;
    
        dos.writeInt(messageType);
        dos.writeUTF(nodeIPAddress);
        dos.writeInt(nodePort);

        dos.flush();
        return baos.toByteArray();
    }
}
