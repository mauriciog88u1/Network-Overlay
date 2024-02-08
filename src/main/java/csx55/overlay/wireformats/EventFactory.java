package csx55.overlay.wireformats;

import csx55.overlay.util.DEBUG;

import java.io.IOException;
import java.nio.ByteBuffer;

public class EventFactory implements Protocol {

    private static volatile EventFactory instance = null;

    private EventFactory() {}

    /**
     * @return the instance of the EventFactory class
     * Purpose of this method is to create a singleton instance of the EventFactory class
     * This is to ensure that only one instance of the EventFactory class is created
     */
    public static EventFactory getInstance() {
        if (instance == null) {
            synchronized (EventFactory.class) {
                if (instance == null) {
                    instance = new EventFactory();
                }
            }
        }
        return instance;
    }


    public  static Event createEvent(byte[] data) throws IOException {
        DEBUG.debug_print("Creating event from data: " + new String(data));
        Event e = null;
        int type = getType(data);
        switch (type) {
            case REGISTER_REQUEST:
                e =  new Register(data);
                break;
            case REGISTER_RESPONSE:
                e = new RegisterResponse(data);
                break;
            case DEREGISTER_REQUEST:
                DEBUG.debug_print("Creating DeregisterRequest");
                break;
            case MESSAGING_NODES_LIST:
                DEBUG.debug_print("Creating MessagingNodesList");
                break;
            case LINK_WEIGHTS:
                DEBUG.debug_print("Creating LinkWeights");
                break;
            case TASK_INITIATE:
                DEBUG.debug_print("Creating TaskInitiate");
                break;
            case TASK_COMPLETE:
                DEBUG.debug_print("Creating TaskComplete");
                break;
            case PULL_TRAFFIC_SUMMARY:
                DEBUG.debug_print("Creating PullTrafficSummary");
                break;
            case TRAFFIC_SUMMARY:
                DEBUG.debug_print("Creating TrafficSummary");
                break;

            default:
                System.out.println("Error: EventFactory: createEvent: unknown event type");
                System.exit(1);
        }
        return e;
    }

    private static int getType(byte[] data) {
        int type = -1;
        try {
            type = ByteBuffer.wrap(data).getInt();
            DEBUG.debug_print("Event type: " + type);
        } catch (Exception e) {
            DEBUG.debug_print("Error: EventFactory: getType: " + e.getMessage());
        }
        return type;
    }





}
