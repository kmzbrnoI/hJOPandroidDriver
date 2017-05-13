package cz.mendelu.xmarik.train_manager;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

import cz.mendelu.xmarik.train_manager.events.LokAddEvent;
import cz.mendelu.xmarik.train_manager.events.LokChangeEvent;
import cz.mendelu.xmarik.train_manager.events.LokEvent;
import cz.mendelu.xmarik.train_manager.events.LokRemoveEvent;
import cz.mendelu.xmarik.train_manager.events.LokRespEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectEvent;
import cz.mendelu.xmarik.train_manager.models.Train;

/**
 * TrainDb is a database of all local trains.
 */

public class TrainDb {
    public static TrainDb instance;

    public Map<Integer, Train> trains = new HashMap<Integer, Train>();

    public TrainDb() {
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onEvent(LokEvent event) {
        try {
            if (event.getParsed().get(3).toUpperCase().equals("AUTH"))
                authEvent(event);
            else if (event.getParsed().get(3).toUpperCase().equals("F"))
                fEvent(event);
            else if (event.getParsed().get(3).toUpperCase().equals("SPD"))
                spdEvent(event);
            else if (event.getParsed().get(3).toUpperCase().equals("RESP"))
                respEvent(event);
            else if (event.getParsed().get(3).toUpperCase().equals("TOTAL"))
                totalEvent(event);
        } catch (Exception e) {
            Log.e("Lok parse", "Error", e);
        }
    }

    public void authEvent(LokEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));

        if (event.getParsed().get(4).toUpperCase().equals("OK") ||
                event.getParsed().get(4).toUpperCase().equals("TOTAL")) {
            if (trains.containsValue(addr)) {
                trains.get(addr).total = event.getParsed().get(4).toUpperCase().equals("TOTAL");
                trains.get(addr).stolen = false;
                if (event.getParsed().size() >= 7)
                    trains.get(addr).updateFromServerString(event.getParsed().get(5));
                EventBus.getDefault().post(new LokChangeEvent(addr));
            } else {
                Train t = new Train(event.getParsed().get(5));
                t.total = event.getParsed().get(4).toUpperCase().equals("TOTAL");
                trains.put(addr, t);
                EventBus.getDefault().post(new LokAddEvent(addr));
            }

        } else if (event.getParsed().get(4).toUpperCase().equals("RELEASE")) {
            if (!trains.containsValue(addr)) return;
            trains.remove(addr);
            EventBus.getDefault().post(new LokRemoveEvent(addr));

        } else if (event.getParsed().get(4).toUpperCase().equals("STOLEN")) {
            if (!trains.containsValue(addr)) return;
            trains.get(addr).stolen = true;
            EventBus.getDefault().post(new LokChangeEvent(addr));
        }
    }

    public void fEvent(LokEvent event) {
        Train t = trains.get(Integer.valueOf(event.getParsed().get(2)));
        String[] f = event.getParsed().get(4).split("-");
        if (f.length == 1) {
            t.function[Integer.valueOf(f[0])].checked = (event.getParsed().get(5) == "1");
        } else if (f.length == 2) {
            int from = Integer.valueOf(f[0]);
            int to = Integer.valueOf(f[1]);

            for (int i = from; i <= to; i++)
                t.function[i].checked = (event.getParsed().get(5).charAt(i-from) == '1');
        }

        EventBus.getDefault().post(new LokChangeEvent(t.addr));
    }

    public void spdEvent(LokEvent event) {
        Train t = trains.get(Integer.valueOf(event.getParsed().get(2)));
        t.kmphSpeed = Integer.valueOf(event.getParsed().get(4));
        t.stepsSpeed = Integer.valueOf(event.getParsed().get(5));
        t.direction = Boolean.valueOf(event.getParsed().get(6));

        EventBus.getDefault().post(new LokChangeEvent(t.addr));
    }

    public void respEvent(LokEvent event) {
        Train t = trains.get(Integer.valueOf(event.getParsed().get(2)));
        if (event.getParsed().get(4).toUpperCase().equals("OK"))
            t.kmphSpeed = Integer.valueOf(event.getParsed().get(5));

        EventBus.getDefault().post(new LokRespEvent(event.getParsed()));
    }

    public void totalEvent(LokEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));
        if (!trains.containsKey(addr)) return;
        Train t = trains.get(addr);
        t.total = event.getParsed().get(4).equals("1");
        EventBus.getDefault().post(new LokChangeEvent(addr));
    }

    @Subscribe
    public void onEvent(TCPDisconnectEvent event) {
        this.trains.clear();
    }
}
