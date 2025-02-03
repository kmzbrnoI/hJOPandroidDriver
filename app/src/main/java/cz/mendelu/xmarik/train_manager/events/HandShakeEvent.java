package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class HandShakeEvent extends ParsedMsgEvent {
    public HandShakeEvent(ArrayList<String> parsed) {
        super(parsed);
    }
}
