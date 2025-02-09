package cz.mendelu.xmarik.train_manager.storage;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import cz.mendelu.xmarik.train_manager.events.TimeEvent;

/**
 * TimeHolder is holder of local model time copy.
 */

public class TimeHolder {
    public static TimeHolder instance;

    public MutableLiveData<String> time = new MutableLiveData<>("");
    public MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> used = new MutableLiveData<>(false);
    public float multiplier = 1;

    private long msTime = 0;
    private long timeLast;
    private final Handler handler = new Handler();
    private final long refreshRate = 200; // ms

    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm:ss");

    public TimeHolder() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected void finalize() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(TimeEvent event) {
        Boolean wasRunning = this.running.getValue();
        timeLast = SystemClock.elapsedRealtime();

        try {
            msTime = timeFormat.parse(event.time).getTime();
        } catch (Exception e) {
            Log.e("TimeHolder", "Unable to convert time into ms: " + event.time);
            return;
        }

        time.postValue(event.time);
        running.postValue(event.running);
        used.postValue(event.used);
        multiplier = event.multiplier;

        if (event.running && (wasRunning == null || !wasRunning)) {
            startTime();
        }
    }

    private void startTime() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long timeNow = SystemClock.elapsedRealtime();
                msTime += Math.round((timeNow - timeLast) * (double) multiplier);
                timeLast = timeNow;

                String newTime = timeFormat.format(new Date(msTime));

                Boolean run = running.getValue();
                if (run != null && run) {
                    time.postValue(newTime);
                    handler.postDelayed(this, refreshRate);
                }
            }
        }, refreshRate);
    }

}
