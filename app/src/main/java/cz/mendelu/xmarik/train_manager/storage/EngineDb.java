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
import cz.mendelu.xmarik.train_manager.models.Engine;

/**
 * EngineDb is a database of all local engines.
 */

public class EngineDb {
    public static EngineDb instance;

    public Map<Integer, Engine> engines = new HashMap<>();

    public EngineDb() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
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
            Engine t = engines.get(addr);
            if (t != null) {
                t.total = event.getParsed().get(4).equalsIgnoreCase("TOTAL");
                t.stolen = false;
                if (event.getParsed().size() >= 7)
                    engines.get(addr).updateFromServerString(event.getParsed().get(5));
                EventBus.getDefault().post(new LokChangeEvent(addr));
            } else {
                t = new Engine(event.getParsed().get(5));
                t.total = event.getParsed().get(4).equalsIgnoreCase("TOTAL");
                engines.put(addr, t);
                EventBus.getDefault().post(new LokAddEvent(addr));
            }

        } else if (event.getParsed().get(4).equalsIgnoreCase("RELEASE") ||
                   event.getParsed().get(4).equalsIgnoreCase("NOT")) {
            if (!engines.containsKey(addr)) return;
            engines.remove(addr);
            EventBus.getDefault().post(new LokRemoveEvent(addr));

        } else if (event.getParsed().get(4).equalsIgnoreCase("STOLEN")) {
            Engine t = engines.get(addr);
            if (t != null) {
                t.stolen = true;
                t.total = false;
                EventBus.getDefault().post(new LokChangeEvent(addr));
            }
        }
    }

    public void fEvent(LokEvent event) {
        Engine t = engines.get(Integer.valueOf(event.getParsed().get(2)));
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
        Engine t = engines.get(Integer.valueOf(event.getParsed().get(2)));
        t.kmphSpeed = Integer.parseInt(event.getParsed().get(4));
        t.stepsSpeed = Integer.parseInt(event.getParsed().get(5));
        t.direction = (event.getParsed().get(6).equals("1") ? Engine.Direction.BACKWARD : Engine.Direction.FORWARD);

        EventBus.getDefault().post(new LokChangeEvent(t.addr));
    }

    public void respEvent(LokEvent event) {
        Engine t = engines.get(Integer.valueOf(event.getParsed().get(2)));
        if (event.getParsed().get(4).equalsIgnoreCase("OK"))
            t.kmphSpeed = Integer.parseInt(event.getParsed().get(6));

        EventBus.getDefault().post(new LokRespEvent(event.getParsed()));
    }

    public void totalEvent(LokEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));
        if (!engines.containsKey(addr)) return;
        Engine t = engines.get(addr);
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
        if (!engines.containsKey(addr)) return;
        Engine t = engines.get(addr);
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
        if (!engines.containsKey(addr)) return;
        Engine t = engines.get(addr);
        String expSpeed = event.getParsed().get(4);
        t.expSpeed = (!expSpeed.equals("-")) ? Integer.parseInt(expSpeed) : -1;
        EventBus.getDefault().post(new LokChangeEvent(addr));
    }

    @Subscribe
    public void onEvent(TCPDisconnectedEvent event) {
        this.engines.clear();
    }
}
