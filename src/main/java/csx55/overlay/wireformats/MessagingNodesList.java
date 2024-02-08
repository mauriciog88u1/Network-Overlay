package csx55.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessagingNodesList implements Event {
    private final int messageType = Protocol.MESSAGING_NODES_LIST;

    private int numNodes;

    private List<String> messagingNodesInfo;

    public MessagingNodesList(int size, List<String> messagingNodesInfo) {
        this.numNodes = size;
        this.messagingNodesInfo = messagingNodesInfo;
    }

    public MessagingNodesList(byte[] marshalledBytes) throws IOException {
        DataInputStream din = new DataInputStream(new java.io.ByteArrayInputStream(marshalledBytes));

        int type = din.readInt();
        if (type != messageType) {
            throw new IllegalArgumentException("Invalid message type for MessagingNodesList");
        }

        int numNodes = din.readInt();
        this.messagingNodesInfo = new ArrayList<>();

        for (int i = 0; i < numNodes; i++) {
            int infoLength = din.readInt();
            byte[] infoBytes = new byte[infoLength];
            din.readFully(infoBytes);
            this.messagingNodesInfo.add(new String(infoBytes));
        }

        din.close();
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(baOutputStream);

        dout.writeInt(this.messageType);
        dout.writeInt(this.messagingNodesInfo.size()); // Number of nodes

        for (String nodeInfo : this.messagingNodesInfo) {
            byte[] infoBytes = nodeInfo.getBytes();
            dout.writeInt(infoBytes.length);
            dout.write(infoBytes);
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

    public List<String> getMessagingNodesInfo() {
        return messagingNodesInfo;
    }
}
