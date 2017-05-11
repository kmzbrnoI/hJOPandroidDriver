package cz.mendelu.xmarik.train_manager.events;

import java.util.ArrayList;

/**
 * Created by JanHoracek on 12.5.2017.
 */

public class GlobalAuthEvent extends GenericEvent {
    public GlobalAuthEvent(ArrayList<String> parsed) {
        super(parsed);
    }
}
