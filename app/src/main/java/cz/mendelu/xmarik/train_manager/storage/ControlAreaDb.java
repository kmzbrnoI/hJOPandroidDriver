package cz.mendelu.xmarik.train_manager.storage;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.AreasClearedEvent;
import cz.mendelu.xmarik.train_manager.events.AreasEvent;
import cz.mendelu.xmarik.train_manager.events.AreasParsedEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;
import cz.mendelu.xmarik.train_manager.models.ControlArea;

/**
 * ControlAreaDb is a class containing list of control areas. It is a singleton.
 */

public class ControlAreaDb {
    public static ControlAreaDb instance;

    public ArrayList<ControlArea> areas = new ArrayList<>();

    public ControlAreaDb() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    protected void finalize() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(AreasEvent event) {
        try {
            this.areas.clear();
            ArrayList<String> areas = ParseHelper.parse(event.getParsed().get(2), "]", "[");

            for (String area : areas) {
                ArrayList<String> parsed = ParseHelper.parse(area, ",", "");
                this.areas.add(new ControlArea(parsed.get(0), parsed.get(1)));
            }
        } catch (Exception e) {
            Log.e("Areas parse", "Error", e);
        }

        EventBus.getDefault().post(new AreasParsedEvent());
    }

    @Subscribe
    public void onEvent(TCPDisconnectedEvent event) {
        this.areas.clear();
        EventBus.getDefault().post(new AreasClearedEvent());
    }
}
