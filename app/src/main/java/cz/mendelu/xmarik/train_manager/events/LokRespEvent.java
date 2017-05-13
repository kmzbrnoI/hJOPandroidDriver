package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class LokRespEvent extends GenericEvent {
    public LokRespEvent(ArrayList<String> parsed) { super(parsed); }
}
