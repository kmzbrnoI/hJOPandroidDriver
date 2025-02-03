package cz.mendelu.xmarik.train_manager.storage;

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
import cz.mendelu.xmarik.train_manager.events.LokTotalChangeErrorEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;
import cz.mendelu.xmarik.train_manager.models.Train;

/**
 * TrainDb is a database of all local trains.
 */

public class TrainDb {
    public static TrainDb instance;

    public Map<Integer, Train> trains = new HashMap<>();

    public TrainDb() {
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onEvent(LokEvent event) {
        try {
            if (event.getParsed().get(3).equalsIgnoreCase("AUTH"))
                authEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("F"))
                fEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("SPD"))
                spdEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("RESP"))
                respEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("TOTAL"))
                totalEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("NAV"))
                expSignEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("EXPECTED-SPEED"))
                expSpdEvent(event);
        } catch (Exception e) {
            Log.e("Lok parse", "Error", e);
        }
    }

    public void authEvent(LokEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));

        if (event.getParsed().get(4).equalsIgnoreCase("OK") ||
                event.getParsed().get(4).equalsIgnoreCase("TOTAL")) {
            Train t = trains.get(addr);
            if (t != null) {
                t.total = event.getParsed().get(4).equalsIgnoreCase("TOTAL");
                t.stolen = false;
                if (event.getParsed().size() >= 7)
                    trains.get(addr).updateFromServerString(event.getParsed().get(5));
                EventBus.getDefault().post(new LokChangeEvent(addr));
            } else {
                t = new Train(event.getParsed().get(5));
                t.total = event.getParsed().get(4).equalsIgnoreCase("TOTAL");
                trains.put(addr, t);
                EventBus.getDefault().post(new LokAddEvent(addr));
            }

        } else if (event.getParsed().get(4).equalsIgnoreCase("RELEASE") ||
                   event.getParsed().get(4).equalsIgnoreCase("NOT")) {
            if (!trains.containsKey(addr)) return;
            trains.remove(addr);
            EventBus.getDefault().post(new LokRemoveEvent(addr));

        } else if (event.getParsed().get(4).equalsIgnoreCase("STOLEN")) {
            Train t = trains.get(addr);
            if (t != null) {
                t.stolen = true;
                t.total = false;
                EventBus.getDefault().post(new LokChangeEvent(addr));
            }
        }
    }

    public void fEvent(LokEvent event) {
        Train t = trains.get(Integer.valueOf(event.getParsed().get(2)));
        String[] f = event.getParsed().get(4).split("-");
        if (f.length == 1) {
            t.function[Integer.parseInt(f[0])].checked = (event.getParsed().get(5).equals("1"));
        } else if (f.length == 2) {
            int from = Integer.parseInt(f[0]);
            int to = Integer.parseInt(f[1]);

            for (int i = from; i <= to; i++)
                t.function[i].checked = (event.getParsed().get(5).charAt(i-from) == '1');
        }

        EventBus.getDefault().post(new LokChangeEvent(t.addr));
    }

    public void spdEvent(LokEvent event) {
        Train t = trains.get(Integer.valueOf(event.getParsed().get(2)));
        t.kmphSpeed = Integer.parseInt(event.getParsed().get(4));
        t.stepsSpeed = Integer.parseInt(event.getParsed().get(5));
        t.direction = (event.getParsed().get(6).equals("1"));

        EventBus.getDefault().post(new LokChangeEvent(t.addr));
    }

    public void respEvent(LokEvent event) {
        Train t = trains.get(Integer.valueOf(event.getParsed().get(2)));
        if (event.getParsed().get(4).equalsIgnoreCase("OK"))
            t.kmphSpeed = Integer.parseInt(event.getParsed().get(6));

        EventBus.getDefault().post(new LokRespEvent(event.getParsed()));
    }

    public void totalEvent(LokEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));
        if (!trains.containsKey(addr)) return;
        Train t = trains.get(addr);
        boolean total = event.getParsed().get(4).equals("1");
        if (t.total == total) {
            EventBus.getDefault().post(new LokChangeEvent(addr));
        } else {
            t.total = total;
            EventBus.getDefault().post(new LokTotalChangeErrorEvent(addr, total));
        }
    }

    public void expSignEvent(LokEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));
        if (!trains.containsKey(addr)) return;
        Train t = trains.get(addr);
        t.expSignalBlock = event.getParsed().get(4);
        try {
            t.expSignalCode = Integer.parseInt(event.getParsed().get(5));
        } catch (NumberFormatException e) {
            t.expSignalCode = -1;
        }
        EventBus.getDefault().post(new LokChangeEvent(addr));
    }

    public void expSpdEvent(LokEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));
        if (!trains.containsKey(addr)) return;
        Train t = trains.get(addr);
        String expSpeed = event.getParsed().get(4);
        t.expSpeed = (!expSpeed.equals("-")) ? Integer.parseInt(expSpeed) : -1;
        EventBus.getDefault().post(new LokChangeEvent(addr));
    }

    @Subscribe
    public void onEvent(TCPDisconnectedEvent event) {
        this.trains.clear();
    }
}
