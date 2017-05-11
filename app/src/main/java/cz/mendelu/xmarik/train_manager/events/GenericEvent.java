package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class GenericEvent {
    private final ArrayList<String> parsed;
    public GenericEvent(ArrayList<String> parsed) {
        this.parsed = parsed;
    }
    public ArrayList<String> getParsed() { return parsed; }
}
