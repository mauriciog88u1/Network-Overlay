package csx55.overlay.wireformats;

public class EventFactory  {
//    singleton instance

    private static EventFactory instance = null;

    private EventFactory() {


    }

    public static EventFactory getInstance() {
        if (instance == null) {
            instance = new EventFactory();
        }
        return instance;
    }

    public Event createEvent(byte[] data) {
        Event e = null;
        int type = getType(data);
        switch (type) {
            case Protocol.REGISTER_REQUEST:
                e = new Register(data);
                break;
            case Protocol.REGISTER_RESPONSE:
                e = new RegisterResponse(data);
                break;
            case Protocol.DEREGISTER_REQUEST:
                e = new Deregister(data);
                break;
            case Protocol.MESSAGING_NODES_LIST:
                e = new MessagingNodesList(data);
                break;
            case Protocol.LINK_WEIGHTS:
                e = new LinkWeights(data);
                break;
            case Protocol.TASK_INITIATE:
                e = new TaskInitiate(data);
                break;
            case Protocol.TASK_COMPLETE:
                e = new TaskComplete(data);
                break;
            case Protocol.PULL_TRAFFIC_SUMMARY:
                e = new PullTrafficSummary(data);
                break;
            case Protocol.TRAFFIC_SUMMARY:
                e = new TrafficSummary(data);
                break;

            default:
                System.out.println("Error: EventFactory: createEvent: unknown event type");
                System.exit(1);
        }
        return e;
    }

    private int getType(byte[] data) {
        int type = -1;
        try {
            type = data[0];
        } catch (Exception e) {
            System.out.println("Error: EventFactory: getType: " + e.getMessage());
            System.exit(1);
        }
        return type;
    }




}
