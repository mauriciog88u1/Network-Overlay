package csx55.overlay.wireformats;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class LinkWeights implements Event {
    private final Map<String, Integer> linkweights;
    private final int messageType = Protocol.LINK_WEIGHTS;

    private final Random random = new Random();

    public LinkWeights() {
        linkweights = new HashMap<>();
    }

    public LinkWeights(byte[] data) throws IOException {
        this.linkweights = new HashMap<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        int type = dis.readInt();
        if (type != messageType) throw new IllegalArgumentException("Invalid message type for LinkWeights");
        int numberOfLinks = dis.readInt();

        for (int i = 0; i < numberOfLinks; i++) {
            String link = dis.readUTF();
            int weight = dis.readInt();
            linkweights.put(link, weight);
        }
    }

    public void generateLinkWeights(ConcurrentHashMap<String, List<String>> overlay) {
        overlay.forEach((node, connections) -> connections.forEach(connection -> {
            String link = node.compareTo(connection) < 0 ? node + "-" + connection : connection + "-" + node;
            if (!linkweights.containsKey(link)) {
                linkweights.put(link, 1 + random.nextInt(10));
            }
        }));
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(messageType);
        dos.writeInt(linkweights.size());

        for (Map.Entry<String, Integer> entry : linkweights.entrySet()) {
            dos.writeUTF(entry.getKey());
            dos.writeInt(entry.getValue());
        }

        dos.flush();
        return baos.toByteArray();
    }

    public Map<String, Integer> getLinkweights() {
        return linkweights;
    }

    @Override
    public int getType() {
        return messageType;
    }
}
