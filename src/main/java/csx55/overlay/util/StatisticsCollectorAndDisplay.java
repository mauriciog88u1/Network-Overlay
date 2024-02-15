package csx55.overlay.util;

import java.util.HashMap;
import java.util.Map;

public class StatisticsCollectorAndDisplay {
    private static class NodeStatistics {
        long sentMessagesSum;
        long receivedMessagesSum;
        int relayedMessages;
        int sendMessages;
        int receivedMessages;
        public NodeStatistics(long sentMessagesSum, long receivedMessagesSum, int relayedMessages, int sendMessages, int receivedMessages) {
            this.sentMessagesSum = sentMessagesSum;
            this.receivedMessagesSum = receivedMessagesSum;
            this.relayedMessages = relayedMessages;
            this.sendMessages = sendMessages;
            this.receivedMessages = receivedMessages;
        }
    }

    private final Map<String, NodeStatistics> statistics = new HashMap<>();

    public synchronized void addNodeStatistics(String nodeIdentifier, long sentSum, long receivedSum, int relayed, int sendMessages, int receivedMessages) {
        statistics.put(nodeIdentifier, new NodeStatistics(sentSum, receivedSum, relayed, sendMessages, receivedMessages));
    }

    public synchronized void displaySummary() {
        System.out.printf("%-20s %-20s %-20s %-20s%n", "Node", "Number of messages sent","Number of messages received","Summation of sent messages","Summation of received messages", "Received Sum", "Number of messages relayed");
        statistics.forEach((nodeIdentifier, stats) ->
                System.out.printf("%-20s %-20d %-20d %-20d%n", nodeIdentifier, stats.sendMessages, stats.receivedMessages, stats.sentMessagesSum, stats.receivedMessagesSum, stats.relayedMessages));
    }

    public synchronized int size() {
        return statistics.size();
    }
}


