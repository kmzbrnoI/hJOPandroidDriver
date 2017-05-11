package cz.mendelu.xmarik.train_manager.events;

/**
 * Created by JanHoracek on 12.5.2017.
 */

public class StringEvent {
    private final String data;
    public StringEvent(String data) {
        this.data = data;
    }
    public String getData() { return data; }
}
