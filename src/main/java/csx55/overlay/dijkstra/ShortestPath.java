package csx55.overlay.dijkstra;

import csx55.overlay.util.DEBUG;

import java.util.*;

public class ShortestPath {

    public List<String> computeShortestPath(Map<String, Map<String, Integer>> graph, String source, String sink) {
        // DEBUG.debug_print("Called computeShortestPath with graph: " + graph + " source: " + source + " sink: " + sink); this works
        Set<String> settledNodes = new HashSet<>();
        Set<String> unsettledNodes = new HashSet<>();
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        DEBUG.debug_print("Computing shortest path from " + source + " to " + sink);
        unsettledNodes.add(source);
        distances.put(source, 0);

        while (!unsettledNodes.isEmpty()) {
            String currentNode = getLowestDistanceNode(unsettledNodes, distances);
//            DEBUG.debug_print("Processing node: " + currentNode + " with current distance: " + distances.get(currentNode));
            unsettledNodes.remove(currentNode);
            Map<String, Integer> adjacentNodes = graph.get(currentNode);
            if (adjacentNodes != null) {
                for (Map.Entry<String, Integer> adjacencyPair : adjacentNodes.entrySet()) {
                    String adjacentNode = adjacencyPair.getKey();
                    Integer edgeWeight = adjacencyPair.getValue();
//                    DEBUG.debug_print("Checking node: " + adjacentNode + " with edge weight: " + edgeWeight);
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
//        DEBUG.debug_print("Lowest distance node: " + lowestDistanceNode);
        return lowestDistanceNode;
    }

    private void calculateMinimumDistance(String evaluationNode, Integer edgeWeight, String sourceNode,
                                          Map<String, Integer> distances, Map<String, String> predecessors) {
        Integer sourceDistance = distances.getOrDefault(sourceNode, Integer.MAX_VALUE);
        if (sourceDistance + edgeWeight < distances.getOrDefault(evaluationNode, Integer.MAX_VALUE)) {
            distances.put(evaluationNode, sourceDistance + edgeWeight);
            predecessors.put(evaluationNode, sourceNode);
//            DEBUG.debug_print("Updated distance for node: " + evaluationNode);
        }
    }

    private List<String> getPath(Map<String, String> predecessors, String sink) {
        LinkedList<String> path = new LinkedList<>();
//        DEBUG.debug_print("Getting path to sink: " + sink);
        String step = sink;
        if (predecessors.get(step) == null) {
            return path;
        }
        path.add(step);
        while (predecessors.containsKey(step)) {
            step = predecessors.get(step);
            path.addFirst(step);
        }
        DEBUG.debug_print("Path: " + path);
        return path;
    }
}