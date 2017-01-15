package cz.mendelu.xmarik.train_manager.events;


public class ServerOkEvent extends GenericEvent{

    public ServerOkEvent(String message) {
        super(message);
    }
}