package csx55.overlay.dijkstra;

import java.util.*;

public class ShortestPath {

    public List<String> computeShortestPath(Map<String, Map<String, Integer>> graph, String source, String sink) {
        Set<String> settledNodes = new HashSet<>();
        Set<String> unsettledNodes = new HashSet<>();
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        unsettledNodes.add(source);
        distances.put(source, 0);

        while (!unsettledNodes.isEmpty()) {
            String currentNode = getLowestDistanceNode(unsettledNodes, distances);
            unsettledNodes.remove(currentNode);
            Map<String, Integer> adjacentNodes = graph.get(currentNode);
            if (adjacentNodes != null) {
                for (Map.Entry<String, Integer> adjacencyPair : adjacentNodes.entrySet()) {
                    String adjacentNode = adjacencyPair.getKey();
                    Integer edgeWeight = adjacencyPair.getValue();
                    if (!settledNodes.contains(adjacentNode)) {
                        calculateMinimumDistance(adjacentNode, edgeWeight, currentNode, distances, predecessors);
                        unsettledNodes.add(adjacentNode);
                    }
                }
            }
            settledNodes.add(currentNode);
        }

        return getPath(predecessors, sink);
    }

    private String getLowestDistanceNode(Set<String> unsettledNodes, Map<String, Integer> distances) {
        String lowestDistanceNode = null;
        int lowestDistance = Integer.MAX_VALUE;
        for (String node : unsettledNodes) {
            int nodeDistance = distances.getOrDefault(node, Integer.MAX_VALUE);
            if (nodeDistance < lowestDistance) {
                lowestDistance = nodeDistance;
                lowestDistanceNode = node;
            }
        }
        return lowestDistanceNode;
    }

    private void calculateMinimumDistance(String evaluationNode, Integer edgeWeight, String sourceNode,
                                          Map<String, Integer> distances, Map<String, String> predecessors) {
        Integer sourceDistance = distances.getOrDefault(sourceNode, Integer.MAX_VALUE);
        if (sourceDistance + edgeWeight < distances.getOrDefault(evaluationNode, Integer.MAX_VALUE)) {
            distances.put(evaluationNode, sourceDistance + edgeWeight);
            predecessors.put(evaluationNode, sourceNode);
        }
    }

    private List<String> getPath(Map<String, String> predecessors, String sink) {
        LinkedList<String> path = new LinkedList<>();
        String step = sink;
        if (predecessors.get(step) == null) {
            return path;
        }
        path.add(step);
        while (predecessors.containsKey(step)) {
            step = predecessors.get(step);
            path.addFirst(step);
        }
        return path;
    }
}
