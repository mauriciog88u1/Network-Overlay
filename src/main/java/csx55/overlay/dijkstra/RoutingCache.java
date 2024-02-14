package csx55.overlay.dijkstra;

import csx55.overlay.util.DEBUG;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoutingCache {
    private Map<String, List<String>> cache;

    public RoutingCache() {
        cache = new HashMap<>();
    }

    public List<String> getPath(String source, String sink) {
        return cache.getOrDefault(source + "->" + sink, null);
    }

    public void addPath(String source, String sink, List<String> path) {
        DEBUG.debug_print("Adding path to cache: " + source + "->" + sink + ": " + path);
        cache.put(source + "->" + sink, path);
    }

    public void printCache(ConcurrentHashMap<String, Map<String, Integer>> networkTopology) {
    
        for (String source : networkTopology.keySet()) {
            for (String destination : networkTopology.keySet()) {
                if (!source.equals(destination)) { 
                    List<String> path = getPath(source, destination);
                    if (path != null && !path.isEmpty()) {
                        printFormattedPath(source, path, networkTopology);
                    }
                }
            }
        }
    }    
    private void printFormattedPath(String source, List<String> path, ConcurrentHashMap<String, Map<String, Integer>> networkTopology) {
        StringBuilder pathStr = new StringBuilder(source);
        String prevNode = source;
        for (String node : path) {
            Integer weight = networkTopology.get(prevNode).get(node);
            pathStr.append("--").append(weight).append("--").append(node);
            prevNode = node;
        }
        System.out.println(pathStr);
    }

}
