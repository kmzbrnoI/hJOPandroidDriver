package cz.mendelu.xmarik.train_manager.network;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import cz.mendelu.xmarik.train_manager.events.AreasEvent;
import cz.mendelu.xmarik.train_manager.events.ConnectionEstablishedEvent;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;
import cz.mendelu.xmarik.train_manager.events.EngineEvent;
import cz.mendelu.xmarik.train_manager.events.RequestEvent;
import cz.mendelu.xmarik.train_manager.events.TCPConnectingErrorEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectReqEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;
import cz.mendelu.xmarik.train_manager.events.TCPRawMsgEvent;
import cz.mendelu.xmarik.train_manager.events.TimeEvent;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;
import cz.mendelu.xmarik.train_manager.models.Server;

/**
 * TCPClient is a basic TCP client that connects to the server and synchronously waits for
 * incoming data. It is intended to be in separate thread.
 */
public class TCPClient {
    static final int TIMER_PING_PERIOD = 1000;

    private String serverHost = "localhost";
    private int serverPort = 0;

    private Socket m_socket = null;

    private ConnectThread m_ct = null;
    private TCPWriteThread m_wt = null;
    private TCPReadThread m_rt = null;

    Timer t_ping = new Timer();

    private static TCPClient instance = null;

    public Server server = null;
    public MutableLiveData<Boolean> dccState = new MutableLiveData<>(null);

    public static final String PROTOCOL_VERSION_CLIENT = "1.1";
    public static final String PROTOCOL_APP_NAME = "hJOPdriver";

    public TCPClient() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        TimerTask pingTask = new PingTask();
        this.t_ping.schedule(pingTask, 0, TIMER_PING_PERIOD);
    }

    protected void finalize() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    public static TCPClient getInstance() {
        if (instance == null)
            instance = new TCPClient();
        return instance;
    }

    public void connect(Server server) {
        this.server = server;
        try {
            this.connect(server.host, server.port);
        } catch (Exception e) {
            this.server = null;
            throw e;
        }
    }

    private void connect(String host, int port) {
        if (this.connecting())
            throw new RuntimeException("Connecting is in progress, wait for it to finish!");
        if (this.connected())
            throw new RuntimeException("Already connected to server!");

        this.serverHost = host;
        this.serverPort = port;

        m_ct = new ConnectThread(); // thread must be recreated once it has finished
        m_ct.start();
    }

    public void disconnect(String reason) {
        this.server = null;
        this.dccState.postValue(null);

        if (m_socket == null)
            return; // already disconnected

        try {
            m_socket.close();
            this.joinSockets();
        } catch (IOException e) {
            Log.e("TCPClient", "disconnect", e);
        }

        m_socket = null;
        EventBus.getDefault().post(new TCPDisconnectedEvent(reason));
    }

    private void joinSockets() {
        try {
            m_wt.interrupt();
            m_wt.join();
        } catch (InterruptedException e) {
            Log.e("TCPClient", "joinSockets", e);
        }

        try {
            m_rt.interrupt();
            m_rt.join();
        } catch (InterruptedException e) {
            Log.e("TCPClient", "joinSockets2", e);
        }
    }

    public boolean connected() {
        return m_socket != null;
    }

    public boolean connecting() {
        return (m_ct != null) && (m_ct.isAlive());
    }

    public void send(String message) {
        try {
            if ((m_socket != null) && ((!m_socket.isConnected()) || (m_wt == null) || (!m_wt.isAlive())))
                throw new RuntimeException("Not connected!");
            m_wt.send((message + '\n').getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e("TCP", "Cannot send data, disconnecting", e);
            this.disconnect(e.getMessage());
        }
    }

    // ConnectThread manages connecting to the server.
    // It does not block main thread while connecting is in progress.
    public class ConnectThread extends Thread {
        public void run() {
            try {
                m_socket = new Socket(serverHost, serverPort);
                m_socket.setTcpNoDelay(true); // disable Nagle's algorithm to make connection low-latency
            } catch (Exception e) {
                m_socket = null;
                Log.e("TCP", "ConnectThread::run", e);
                EventBus.getDefault().post(new TCPConnectingErrorEvent(e.getMessage()));
                return;
            }

            m_wt = new TCPWriteThread(m_socket);
            m_wt.start();
            m_rt = new TCPReadThread(m_socket);
            m_rt.start();

            EventBus.getDefault().post(new ConnectionEstablishedEvent());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    private void onConnectingError(TCPConnectingErrorEvent event) {
        this.server = null;
        this.dccState.postValue(null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    private void onDisconnectRequest(TCPDisconnectReqEvent event) {
        this.disconnect(event.reason);
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
                EventBus.getDefault().post(new EngineEvent(parsed));
        }
    }

    class PingTask extends TimerTask {
        public void run() {
            if (TCPClient.this.connected())
                TCPClient.this.send("-;PING;");
        }
    }
}
