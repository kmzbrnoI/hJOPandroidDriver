package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by ja on 8. 12. 2016.
 */

public class TrainReloadEvent extends GenericEvent{

    public TrainReloadEvent(String message) {
        super(message);
    }
}