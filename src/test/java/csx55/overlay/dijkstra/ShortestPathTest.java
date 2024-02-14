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
}
