package csx55.overlay.dijkstra;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShortestPathTest {

    @Test
    @DisplayName("Testing Shortest path to verify it works correctly")
    void testComputeShortestPath() {
        System.out.println("testing");
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        System.out.println("testing");
        graph.put("A", Map.of("B", 1, "C", 4));
        graph.put("B", Map.of("A", 1, "C", 2, "D", 5));
        graph.put("C", Map.of("A", 4, "B", 2, "D", 1));
        graph.put("D", Map.of("B", 5, "C", 1));

        ShortestPath shortestPath = new ShortestPath();

        List<String> path = shortestPath.computeShortestPath(graph, "A", "D");

        List<String> expectedPath = List.of("A", "B", "C", "D");
        Assertions.assertEquals(expectedPath, path, "The computed shortest path does not match the expected path.");
    }
    @DisplayName("Testing Shortest path to verify it works correctly with 10 nodes with weights from1 to 10")
    @Test
    void testComputeShortestPath2() {
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        graph.put("A", Map.of("B", 1, "C", 4));
        graph.put("B", Map.of("A", 1, "C", 2, "D", 5));
        graph.put("C", Map.of("A", 4, "B", 2, "D", 1));
        graph.put("D", Map.of("B", 5, "C", 1));
        graph.put("E", Map.of("B", 5, "C", 1));
        graph.put("F", Map.of("B", 5, "C", 1));
        graph.put("G", Map.of("B", 5, "C", 1));
        graph.put("H", Map.of("B", 5, "C", 1));
        graph.put("I", Map.of("B", 5, "C", 1));
        graph.put("J", Map.of("B", 5, "C", 1));

        ShortestPath shortestPath = new ShortestPath();
        shortestPath.computeShortestPath(graph, "A", "D");
        List<String> path = shortestPath.computeShortestPath(graph, "A", "D");
        List<String> expectedPath = List.of("A", "B", "C", "D");
        Assertions.assertEquals(expectedPath, path, "The computed shortest path does not match the expected path.");


    }
    @Test
    void testComputeNonSequentialShortestPath() {
        Map<String, Map<String, Integer>> graph = new HashMap<>();

        for (int i = 1; i <= 20; i++) {
            graph.put("Node" + i, new HashMap<>());
        }

        graph.get("Node1").put("Node2", 6);
        graph.get("Node2").put("Node3", 3);
        graph.get("Node3").put("Node4", 2);
        graph.get("Node4").put("Node5", 4);
        graph.get("Node5").put("Node10", 1);
        graph.get("Node10").put("Node15", 2);
        graph.get("Node15").put("Node20", 3);
        graph.get("Node1").put("Node6", 1);
        graph.get("Node6").put("Node8", 1);
        graph.get("Node8").put("Node15", 1);
        graph.get("Node2").put("Node7", 5);
        graph.get("Node7").put("Node14", 2);
        graph.get("Node14").put("Node19", 2);
        graph.get("Node19").put("Node20", 1);
        ShortestPath shortestPath = new ShortestPath();

        List<String> path = shortestPath.computeShortestPath(graph, "Node1", "Node20");

        List<String> expectedPath = List.of("Node1", "Node6", "Node8", "Node15", "Node20");

        Assertions.assertEquals(expectedPath, path, "The computed shortest path does not match the expected path.");
    }



}
