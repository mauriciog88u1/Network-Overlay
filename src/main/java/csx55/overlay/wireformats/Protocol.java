package csx55.overlay.wireformats;

public class Protocol {
    private Protocol() {}

    public static final int REGISTER_REQUEST = 1;
    public static final int REGISTER_RESPONSE = 2;
    public static final int DEREGISTER_REQUEST = 3;
    public static final int MESSAGING_NODES_LIST = 4;
    public static final int LINK_WEIGHTS = 5;
    public static final int TASK_INITIATE = 6;
    public static final int TASK_COMPLETE = 7;
    public static final int PULL_TRAFFIC_SUMMARY = 8;
    public static final int TRAFFIC_SUMMARY = 9;
}
