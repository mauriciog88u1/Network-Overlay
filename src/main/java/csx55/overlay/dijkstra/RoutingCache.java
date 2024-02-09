package csx55.overlay.dijkstra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutingCache {
    private Map<String, List<String>> cache;

    public RoutingCache() {
        cache = new HashMap<>();
    }

    public List<String> getPath(String source, String sink) {
        return cache.getOrDefault(source + "->" + sink, null);
    }

    public void addPath(String source, String sink, List<String> path) {
        cache.put(source + "->" + sink, path);
    }

    public void printCache() {
        for (Map.Entry<String, List<String>> entry : cache.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
