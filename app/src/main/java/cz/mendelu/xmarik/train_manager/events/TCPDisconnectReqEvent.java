package cz.mendelu.xmarik.train_manager.events;

public class TCPDisconnectReqEvent {
    final public String reason;
    public TCPDisconnectReqEvent(String reason) {
        this.reason = reason;
    }
}
