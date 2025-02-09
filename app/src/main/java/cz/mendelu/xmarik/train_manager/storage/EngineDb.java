package cz.mendelu.xmarik.train_manager.storage;

import static cz.mendelu.xmarik.train_manager.models.Engine.EXP_SPEED_UNKNOWN;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

import cz.mendelu.xmarik.train_manager.events.EngineAddEvent;
import cz.mendelu.xmarik.train_manager.events.EngineChangeEvent;
import cz.mendelu.xmarik.train_manager.events.EngineEvent;
import cz.mendelu.xmarik.train_manager.events.EngineRemoveEvent;
import cz.mendelu.xmarik.train_manager.events.EngineRespEvent;
import cz.mendelu.xmarik.train_manager.events.EngineTotalChangeErrorEvent;
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

    @Override
    protected void finalize() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(EngineEvent event) {
        try {
            if (event.getParsed().get(3).equalsIgnoreCase("AUTH"))
                this.authEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("F"))
                this.fEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("SPD"))
                this.spdEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("RESP"))
                this.respEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("TOTAL"))
                this.totalEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("NAV"))
                this.expSignEvent(event);
            else if (event.getParsed().get(3).equalsIgnoreCase("EXPECTED-SPEED"))
                this.expSpdEvent(event);
        } catch (Exception e) {
            Log.e("Lok parse", "Error", e);
        }
    }

    public void authEvent(EngineEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));

        if (event.getParsed().get(4).equalsIgnoreCase("OK") ||
                event.getParsed().get(4).equalsIgnoreCase("TOTAL")) {
            Engine e = engines.get(addr);
            if (e != null) {
                e.total = event.getParsed().get(4).equalsIgnoreCase("TOTAL");
                e.stolen = false;
                if (event.getParsed().size() >= 7)
                    e.updateFromServerString(event.getParsed().get(5));
                EventBus.getDefault().post(new EngineChangeEvent(addr));
            } else {
                e = new Engine(event.getParsed().get(5));
                e.total = event.getParsed().get(4).equalsIgnoreCase("TOTAL");
                engines.put(addr, e);
                EventBus.getDefault().post(new EngineAddEvent(addr));
            }

        } else if (event.getParsed().get(4).equalsIgnoreCase("RELEASE") ||
                   event.getParsed().get(4).equalsIgnoreCase("NOT")) {
            if (!engines.containsKey(addr))
                return;
            engines.remove(addr);
            EventBus.getDefault().post(new EngineRemoveEvent(addr));

        } else if (event.getParsed().get(4).equalsIgnoreCase("STOLEN")) {
            Engine e = engines.get(addr);
            if (e != null) {
                e.stolen = true;
                e.total = false;
                EventBus.getDefault().post(new EngineChangeEvent(addr));
            }
        }
    }

    public void fEvent(EngineEvent event) {
        Engine e = engines.get(Integer.valueOf(event.getParsed().get(2)));
        if (e == null)
            return;
        String[] f = event.getParsed().get(4).split("-");
        if (f.length == 1) {
            e.function[Integer.parseInt(f[0])].checked = (event.getParsed().get(5).equals("1"));
        } else if (f.length == 2) {
            int from = Integer.parseInt(f[0]);
            int to = Integer.parseInt(f[1]);

            for (int i = from; i <= to; i++)
                e.function[i].checked = (event.getParsed().get(5).charAt(i-from) == '1');
        }

        EventBus.getDefault().post(new EngineChangeEvent(e.addr));
    }

    public void spdEvent(EngineEvent event) {
        Engine e = engines.get(Integer.valueOf(event.getParsed().get(2)));
        if (e == null)
            return;

        e.kmphSpeed = Integer.parseInt(event.getParsed().get(4));
        e.stepsSpeed = Integer.parseInt(event.getParsed().get(5));
        e.direction = (event.getParsed().get(6).equals("1") ? Engine.Direction.BACKWARD : Engine.Direction.FORWARD);

        EventBus.getDefault().post(new EngineChangeEvent(e.addr));
    }

    public void respEvent(EngineEvent event) {
        Engine e = engines.get(Integer.valueOf(event.getParsed().get(2)));
        if (e == null)
            return;

        if (event.getParsed().get(4).equalsIgnoreCase("OK"))
            e.kmphSpeed = Integer.parseInt(event.getParsed().get(6));

        EventBus.getDefault().post(new EngineRespEvent(event.getParsed()));
    }

    public void totalEvent(EngineEvent event) {
        Engine e = engines.get(Integer.parseInt(event.getParsed().get(2)));
        if (e == null)
            return;

        boolean total = event.getParsed().get(4).equals("1");
        if (e.total == total) {
            EventBus.getDefault().post(new EngineChangeEvent(e.addr));
        } else {
            e.total = total;
            EventBus.getDefault().post(new EngineTotalChangeErrorEvent(e.addr, total));
        }
    }

    public void expSignEvent(EngineEvent event) {
        int addr = Integer.parseInt(event.getParsed().get(2));
        Engine engine = engines.get(addr);
        if (engine == null)
            return;

        engine.expSignalBlock = event.getParsed().get(4);
        try {
            engine.expSignalCode = Integer.parseInt(event.getParsed().get(5));
        } catch (NumberFormatException e) {
            engine.expSignalCode = -1;
        }
        EventBus.getDefault().post(new EngineChangeEvent(engine.addr));
    }

    public void expSpdEvent(EngineEvent event) {
        Engine e = engines.get(Integer.parseInt(event.getParsed().get(2)));
        if (e == null)
            return;

        String expSpeed = event.getParsed().get(4);
        e.expSpeed = (!expSpeed.equals("-")) ? Integer.parseInt(expSpeed) : EXP_SPEED_UNKNOWN;

        EventBus.getDefault().post(new EngineChangeEvent(e.addr));
    }

    @Subscribe
    public void onEvent(TCPDisconnectedEvent event) {
        this.engines.clear();
    }
}
