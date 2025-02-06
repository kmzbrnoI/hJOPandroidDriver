package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

public class EngineRespEvent extends ParsedMsgEvent {
    public EngineRespEvent(ArrayList<String> parsed) { super(parsed); }
}
