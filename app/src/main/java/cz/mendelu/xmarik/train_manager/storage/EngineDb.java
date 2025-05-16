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
            if (event.getParsed().get(3).equalsIgnoreCase("AUTH")) {
                this.authEvent(event);
            } else {
                Engine engine = engines.get(Integer.parseInt(event.getParsed().get(2)));
                if (engine == null)
                    return;

                if (event.getParsed().get(3).equalsIgnoreCase("F"))
                    engine.fEvent(event.getParsed());
                else if (event.getParsed().get(3).equalsIgnoreCase("SPD"))
                    engine.spdEvent(event.getParsed());
                else if (event.getParsed().get(3).equalsIgnoreCase("RESP"))
                    engine.respEvent(event.getParsed());
                else if (event.getParsed().get(3).equalsIgnoreCase("TOTAL"))
                    engine.totalEvent(event.getParsed());
                else if (event.getParsed().get(3).equalsIgnoreCase("NAV"))
                    engine.expSignalEvent(event.getParsed());
                else if (event.getParsed().get(3).equalsIgnoreCase("EXPECTED-SPEED"))
                    engine.expSpdEvent(event.getParsed());
            }
        } catch (Exception e) {
            Log.e("Lok parse", "Error", e);
        }
    }

    public void authEvent(EngineEvent event) {
        final int addr = Integer.parseInt(event.getParsed().get(2));

        if (event.getParsed().get(4).equalsIgnoreCase("OK") ||
                event.getParsed().get(4).equalsIgnoreCase("TOTAL")) {
            Engine e = engines.get(addr);
            if (e != null) {
                e.total = event.getParsed().get(4).equalsIgnoreCase("TOTAL");
                e.stolen = false;
                if (event.getParsed().size() > 5)
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

    @Subscribe
    public void onEvent(TCPDisconnectedEvent event) {
        this.engines.clear();
    }

    public boolean isAnyEngineMultitrack() {
        for (Engine e : this.engines.values())
            if (e.multitrack)
                return true;
        return false;
    }
}
