package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 20. 3. 2017.
 */

public class CriticalErrorEvent extends GenericEvent {

    public CriticalErrorEvent(String message) {
        super(message);
    }
}
