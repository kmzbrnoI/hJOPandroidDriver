package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 26. 11. 2016.
 */

public class GenericEvent {
    private final String message;

    public GenericEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
