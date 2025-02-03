package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class ParsedMsgEvent {
    private final ArrayList<String> parsed;
    public ParsedMsgEvent(ArrayList<String> parsed) {
        this.parsed = parsed;
    }
    public ArrayList<String> getParsed() { return parsed; }
}
