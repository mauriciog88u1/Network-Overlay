package csx55.overlay.util;

import csx55.overlay.node.Node;

import java.util.concurrent.ConcurrentHashMap;

public class OverlayCreator {
    public void createOverlay(int numberOfConnections, ConcurrentHashMap<String, Node> registeredNodes) {
//        set as 1 -> 2 -> 3 -> 4 to avoid parititons
//        int i = 0;
//        for (String node : registeredNodes.keySet()) {
//            if (i == registeredNodes.size() - 1) {
//                registeredNodes.get(node);
//            } else {
//                registeredNodes.get(node).setNextNode(registeredNodes.keySet().toArray()[i + 1].toString());
//            }
//            i++;
//        }



    }



}
