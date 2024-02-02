package csx55.overlay.wireformats;

public interface Protocol {

public final int REGISTER_REQUEST = 1;

public final int REGISTER_RESPONSE = 2;

public final int DEREGISTER_REQUEST = 3;

public final int DEREGISTER_RESPONSE = 4;

public final int MESSAGING_NODES_LIST = 5;

public final int LINK_WEIGHTS = 6;

public final int TASK_INITIATE = 7;

public final int TASK_COMPLETE = 8;

public final int PULL_TRAFFIC_SUMMARY = 9;

public final int TRAFFIC_SUMMARY = 10;

}