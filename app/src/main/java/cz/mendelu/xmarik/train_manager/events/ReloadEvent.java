package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 14. 10. 2016.
 */

public class ReloadEvent {

    private final String message;

    public ReloadEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
