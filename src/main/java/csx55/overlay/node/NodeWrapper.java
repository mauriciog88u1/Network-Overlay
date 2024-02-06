package csx55.overlay.node;

public class NodeWrapper {
    private String hostname;
    private String ip;
    private int port;

    public NodeWrapper(String hostname, String ip, int port) {
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

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }
    @Override
    public String toString() {

        return "NodeWrapper{" +
                "hostname='" + hostname + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
