package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class LokEvent extends GenericEvent {
    public LokEvent(ArrayList<String> parsed) { super(parsed); }
}
