package csx55.overlay.node;

import csx55.overlay.util.DEBUG;

public class Node {

    private String hostname;
    private String ip;
    private int port;

    public Node(String hostname, String ip, int port) {
        DEBUG.debug_print("Node created: " + hostname + " " + ip + " " + port);
        this.hostname = hostname;
        this.ip = ip;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }


    public void onEvent(Object event) {
        DEBUG.debug_print("Event received: " + event.toString());
    }

}
