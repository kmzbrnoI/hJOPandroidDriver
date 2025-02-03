package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class RequestEvent extends ParsedMsgEvent {
    public RequestEvent(ArrayList<String> parsed) {
        super(parsed);
    }
}
