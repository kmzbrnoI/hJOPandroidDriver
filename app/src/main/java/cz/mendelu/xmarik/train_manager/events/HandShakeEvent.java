package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class HandShakeEvent extends GenericEvent {
    public HandShakeEvent(ArrayList<String> parsed) {
        super(parsed);
    }
}
