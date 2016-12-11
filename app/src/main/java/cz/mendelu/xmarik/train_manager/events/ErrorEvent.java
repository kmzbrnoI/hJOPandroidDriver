package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 11. 12. 2016.
 */

public class ErrorEvent extends GenericEvent {
    public ErrorEvent(String message) {
        super(message);
    }
}
