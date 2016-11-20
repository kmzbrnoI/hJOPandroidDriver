package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 14. 10. 2016.
 */

public class HandShakeEvent {

    private final String message;

    public HandShakeEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
