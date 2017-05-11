package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 11. 12. 2016.
 */

public class TCPErrorEvent {
    private final String error;
    public TCPErrorEvent(String error) {
        this.error = error;
    }
    public String getError() { return error; }
}
