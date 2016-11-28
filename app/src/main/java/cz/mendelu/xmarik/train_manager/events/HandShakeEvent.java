package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 14. 10. 2016.
 */

public class HandShakeEvent extends GenericEvent{

    public HandShakeEvent(String message) {
        super(message);
    }
}
