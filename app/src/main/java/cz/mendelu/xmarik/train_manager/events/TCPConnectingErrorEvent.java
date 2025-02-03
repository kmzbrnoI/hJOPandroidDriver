package cz.mendelu.xmarik.train_manager.events;

public class TCPConnectingErrorEvent {
    private final String error;
    public TCPConnectingErrorEvent(String error) {
        this.error = error;
    }
    public String getError() { return error; }
}

