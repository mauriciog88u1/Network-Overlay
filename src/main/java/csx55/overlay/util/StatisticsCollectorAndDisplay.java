package csx55.overlay.util;

import java.util.HashMap;
import java.util.Map;

public class StatisticsCollectorAndDisplay {
    private static class NodeStatistics {
        long sentMessagesSum;
        long receivedMessagesSum;
        int relayedMessages;
        public NodeStatistics(long sentMessagesSum, long receivedMessagesSum, int relayedMessages) {
            this.sentMessagesSum = sentMessagesSum;
            this.receivedMessagesSum = receivedMessagesSum;
            this.relayedMessages = relayedMessages;
        }
    }

    private final Map<String, NodeStatistics> statistics = new HashMap<>();

    public synchronized void addNodeStatistics(String nodeIdentifier, long sentSum, long receivedSum, int relayed) {
        statistics.put(nodeIdentifier, new NodeStatistics(sentSum, receivedSum, relayed));
    }

    public synchronized void displaySummary() {
        System.out.printf("%-20s %-20s %-20s %-20s%n", "Node", "Sent Sum", "Received Sum", "Relayed");
        statistics.forEach((nodeIdentifier, stats) ->
                System.out.printf("%-20s %-20d %-20d %-20d%n", nodeIdentifier, stats.sentMessagesSum, stats.receivedMessagesSum, stats.relayedMessages));
    }

    public synchronized int size() {
        return statistics.size();
    }
}


