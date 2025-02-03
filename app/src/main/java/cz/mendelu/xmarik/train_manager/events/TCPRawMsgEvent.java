package cz.mendelu.xmarik.train_manager.events;

public class TCPRawMsgEvent {
    public final String message;
    public TCPRawMsgEvent(String msg) {
        message = msg;
    }
}
