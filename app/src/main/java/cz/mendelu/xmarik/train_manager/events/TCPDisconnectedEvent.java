package cz.mendelu.xmarik.train_manager.events;

public class TCPDisconnectedEvent {
    private final String error;
    public TCPDisconnectedEvent(String error) {
        this.error = error;
    }
    public String getError() { return error; }
}
