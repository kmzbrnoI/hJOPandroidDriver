package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class GlobalAuthEvent extends ParsedMsgEvent {
    public GlobalAuthEvent(ArrayList<String> parsed) {
        super(parsed);
    }
}
