package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class EngineEvent extends ParsedMsgEvent {
    public EngineEvent(ArrayList<String> parsed) { super(parsed); }
}
