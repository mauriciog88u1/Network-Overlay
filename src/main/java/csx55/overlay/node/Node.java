package csx55.overlay.node;

import csx55.overlay.util.DEBUG;
import csx55.overlay.wireformats.Event;

public interface Node {
//     Node [Interface with the onEvent(Event) method]
        void onEvent(Event event);
        String getHostname();
        String getIp();
        int getPort();



}