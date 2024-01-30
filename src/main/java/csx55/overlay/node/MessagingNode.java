package csx55.overlay.node;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.wireformats.Register;

import static csx55.overlay.util.DEBUG.debug_print;

public class MessagingNode {


    public MessagingNode(String registryHost, int registryPort) {
        debug_print("MessagingNode created: " + registryHost + " " + registryPort);


    }

    public static void main(String[] args) {
        String registryHost = args[0];
        int registryPort = Integer.parseInt(args[1]);
        MessagingNode messagingNode = new MessagingNode(registryHost, registryPort);
        messagingNode.start();

    }

    private void start() {
        debug_print("MessagingNode started.");
]

    }
}
