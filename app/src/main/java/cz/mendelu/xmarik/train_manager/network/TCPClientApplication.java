package cz.mendelu.xmarik.train_manager.network;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.ConnectException;
import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.AreasEvent;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;
import cz.mendelu.xmarik.train_manager.events.LokEvent;
import cz.mendelu.xmarik.train_manager.events.RequestEvent;
import cz.mendelu.xmarik.train_manager.events.TCPConnectingErrorEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectReqEvent;
import cz.mendelu.xmarik.train_manager.events.TCPRawMsgEvent;
import cz.mendelu.xmarik.train_manager.events.TimeEvent;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;
import cz.mendelu.xmarik.train_manager.models.Server;

/**
 * TCPClientApplication is a singleton, whicch handles connection with the server. It encapsules
 * TCPClient.
 */

public class TCPClientApplication extends Application {
    static TCPClientApplication instance;

    public Server server = null;
    public MutableLiveData<Boolean> dccState = new MutableLiveData<>(null);

    TCPClient mTcpClient = new TCPClient();

    TCPClientApplication() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    protected void finalize() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    public static TCPClientApplication getInstance() {
        if (instance == null) instance = new TCPClientApplication();
        return instance;
    }

    public void connect(Server server) {
        this.server = server;
        try {
            mTcpClient.connect(server.host, server.port);
        } catch (Exception e) {
            this.server = null;
            throw e;
        }
    }

    public void disconnect(String reason) {
        this.server = null;
        this.dccState.postValue(null);
        this.mTcpClient.disconnect(reason);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectingError(TCPConnectingErrorEvent event) {
        this.server = null;
        this.dccState.postValue(null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void disconnect(TCPDisconnectReqEvent event) {
        this.disconnect(event.reason);
    }

    public void send(String message) {
        try {
            mTcpClient.send(message);
        } catch (ConnectException e) {
            Log.e("TCP", "Cannot send data, disconnecting", e);
            this.disconnect(e.getMessage());
        }
    }

    public boolean connected() {
        return mTcpClient.connected();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageReceived(TCPRawMsgEvent event) {
        ArrayList<String> parsed = ParseHelper.parse(event.message, ";", "");

        if (parsed.size() < 2 || !parsed.get(0).equals("-")) return;
        parsed.set(1, parsed.get(1).toUpperCase());

        if (parsed.get(1).equals("HELLO")) {
            EventBus.getDefault().post(new HandShakeEvent(parsed));

        } else if (parsed.get(1).equals("OR-LIST")) {
            EventBus.getDefault().post(new AreasEvent(parsed));

        } else if ((parsed.get(1).equals("DCC")) && (parsed.size() > 2)) {
            this.dccState.postValue(parsed.get(2).equals("GO"));

        } else if ((parsed.get(1).equals("MOD-CAS") && (parsed.size() > 2))) {
            EventBus.getDefault().post(new TimeEvent(parsed));

        } else if ((parsed.get(1).equals("PING")) && (parsed.size() > 2) && (parsed.get(2).equalsIgnoreCase("REQ-RESP"))) {
            if (parsed.size() >= 4) {
                this.send("-;PONG;"+parsed.get(3)+'\n');
            } else {
                this.send("-;PONG\n");
            }

        } else if (parsed.get(1).equals("LOK")) {
            if (parsed.size() < 3) return;
            if (parsed.get(2).equals("G")) {
                if (parsed.get(3).equalsIgnoreCase("AUTH"))
                    EventBus.getDefault().post(new GlobalAuthEvent(parsed));
                else if (parsed.get(3).equalsIgnoreCase("PLEASE-RESP"))
                    EventBus.getDefault().post(new RequestEvent(parsed));
            } else
                EventBus.getDefault().post(new LokEvent(parsed));
        }

    }
}
