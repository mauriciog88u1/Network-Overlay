package csx55.overlay.dijkstra;

import csx55.overlay.node.NodeWrapper;

import java.util.*;

public class ShortestPath {

    public List<String> computeShortestPath(Map<String, NodeWrapper> graph, String source, String sink) {
        Set<String> settledNodes = new HashSet<>();
        Set<String> unsettledNodes = new HashSet<>();
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        unsettledNodes.add(source);
        distances.put(source, 0);

        while (!unsettledNodes.isEmpty()) {
            String currentNode = getLowestDistanceNode(unsettledNodes, distances);
            unsettledNodes.remove(currentNode);
            for (Map.Entry<String, Integer> adjacencyPair : graph.get(currentNode).getAdjacentNodes().entrySet()) {
                String adjacentNode = adjacencyPair.getKey();
                Integer edgeWeight = adjacencyPair.getValue();
                if (!settledNodes.contains(adjacentNode)) {
                    calculateMinimumDistance(adjacentNode, edgeWeight, currentNode, distances, predecessors);
                    unsettledNodes.add(adjacentNode);
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
            int nodeDistance = distances.get(node);
            if (nodeDistance < lowestDistance) {
                lowestDistance = nodeDistance;
                lowestDistanceNode = node;
            }
        }
        return lowestDistanceNode;
    }

    private void calculateMinimumDistance(String evaluationNode, Integer edgeWeigh, String sourceNode,
                                          Map<String, Integer> distances, Map<String, String> predecessors) {
        Integer sourceDistance = distances.get(sourceNode);
        if (sourceDistance + edgeWeigh < distances.getOrDefault(evaluationNode, Integer.MAX_VALUE)) {
            distances.put(evaluationNode, sourceDistance + edgeWeigh);
            predecessors.put(evaluationNode, sourceNode);
        }
    }

    private List<String> getPath(Map<String, String> predecessors, String sink) {
        List<String> path = new LinkedList<>();
        String step = sink;
        if (predecessors.get(step) == null) {
            return path; // Path not found
        }
        path.add(step);
        while (predecessors.get(step) != null) {
            step = predecessors.get(step);
            path.add(0, step);
        }
        return path;
    }
}
