package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 20. 3. 2017.
 */

public class CriticalErrorEvent {
    private final String error;
    public CriticalErrorEvent(String error) {
        this.error = error;
    }
    public String getData() { return error; }
}
