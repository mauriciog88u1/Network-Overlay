package csx55.overlay.util;

import csx55.overlay.node.NodeWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OverlayCreator {

    public ConcurrentHashMap<String, List<String>> createOverlay(ConcurrentHashMap<String, NodeWrapper> registeredNodes, int numberOfConnections) {
        List<String> nodeKeys = new ArrayList<>(registeredNodes.keySet());

        if (nodeKeys.size() < numberOfConnections + 1) {
            throw new IllegalArgumentException("Not enough nodes to form a ring with " + numberOfConnections + " connections.");
        }

        ConcurrentHashMap<String, List<String>> overlayMap = new ConcurrentHashMap<>();
            // 1 -> 2 -> 3 -> 4 -> 1 toplogy lol
        for (int i = 0; i < nodeKeys.size(); i++) {
            List<String> connections = new ArrayList<>();
            for (int j = 1; j <= numberOfConnections; j++) {
                int connectIndex = (i + j) % nodeKeys.size();
                connections.add(nodeKeys.get(connectIndex));
            }
            overlayMap.put(nodeKeys.get(i), connections);
        }

        return overlayMap;
    }

}
