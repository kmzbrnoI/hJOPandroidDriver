package cz.mendelu.xmarik.train_manager.network;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.net.ConnectException;
import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.AreasEvent;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;
import cz.mendelu.xmarik.train_manager.events.LokEvent;
import cz.mendelu.xmarik.train_manager.events.RequestEvent;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;

/**
 * TCPClientApplication is a singleton, whicch handles connection with the server. It encapsules
 * TCPClient.
 */

public class TCPClientApplication extends Application {
    static TCPClientApplication instance;

    public Server server = null;

    public boolean auth = false;
    TCPClient mTcpClient;
    private ListenTask listenTask;

    public static TCPClientApplication getInstance() {
        if (instance == null) instance = new TCPClientApplication();
        return instance;
    }

    public void connect(Server server) {
        this.server = server;
        mTcpClient = new TCPClient(server.host, server.port);
        listenTask = new ListenTask();
        listenTask.execute();
    }

    public void disconnect() {
        this.server = null;

        if (mTcpClient != null)
            this.mTcpClient.disconnect();
        if (listenTask != null)
            listenTask.cancel(true);
    }

    public void send(String message) {
        if (mTcpClient == null) return;

        try {
            mTcpClient.send(message);
        } catch (ConnectException e) {
            Log.e("TCP", "Cannot send data, disconnecting", e);
            this.disconnect();
        }
    }

    public boolean connected() { return (mTcpClient != null && mTcpClient.connected()); }

    public class ListenTask extends AsyncTask<String, String, TCPClient> {
        @Override
        protected TCPClient doInBackground(String... message) {

            mTcpClient.listen(new TCPClient.OnMessageReceived()  {
                @Override
                public void messageReceived(String message) {
                    publishProgress(message);
                }
            });

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            ArrayList<String> parsed = ParseHelper.parse(values[0], ";", "");

            if (parsed.size() < 2 || !parsed.get(0).equals("-")) return;
            parsed.set(1, parsed.get(1).toUpperCase());

            if (parsed.get(1).equals("HELLO")) {
                EventBus.getDefault().post(new HandShakeEvent(parsed));

            } else if (parsed.get(1).equals("OR-LIST")) {
                EventBus.getDefault().post(new AreasEvent(parsed));

            } else if (parsed.get(1).equals("LOK")) {
                if (parsed.size() < 3) return;
                if (parsed.get(2).equals("G")) {
                    if (parsed.get(3).toUpperCase().equals("AUTH"))
                        EventBus.getDefault().post(new GlobalAuthEvent(parsed));
                    else if (parsed.get(3).toUpperCase().equals("PLEASE-RESP"))
                        EventBus.getDefault().post(new RequestEvent(parsed));
                } else
                    EventBus.getDefault().post(new LokEvent(parsed));
            }
        }
    }

}
