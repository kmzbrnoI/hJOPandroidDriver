package cz.mendelu.xmarik.train_manager.events;

import android.util.Log;

import java.util.ArrayList;

/**
 * TimeEvent received periodically and when the model time props are changed
 * https://github.com/kmzbrnoI/hJOPserver/wiki/panelServer-mc
 */

public class TimeEvent extends GenericEvent {

    public boolean running;
    public float multiplier;
    public String time;
    public boolean used;

    public TimeEvent(ArrayList<String> parsed) {
        super(parsed);
        this.running = (parsed.size() > 2 && parsed.get(2).equals("1"));
        try {
            this.multiplier = parsed.size() > 3 ? Float.parseFloat(parsed.get(3)) : 1;
        } catch (NumberFormatException e) {
            Log.e("TimeEvent", "Unable to parse multiplier: " + parsed.get(3));
        }
        this.time = parsed.size() > 4 ? parsed.get(4) : "";
        this.used = (parsed.size() < 6 || parsed.get(5).equals("1")); // optional value
    }

}
