package cz.mendelu.xmarik.train_manager.events;

public class TCPDisconnectEvent {
    private final String error;
    public TCPDisconnectEvent(String error) {
        this.error = error;
    }
    public String getError() { return error; }
}
