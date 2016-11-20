package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 20. 10. 2016.
 */

public class RefuseEvent {
    private final String message;

    public RefuseEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
