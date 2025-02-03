package cz.mendelu.xmarik.train_manager.network;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import cz.mendelu.xmarik.train_manager.events.ConnectionEstablishedEvent;
import cz.mendelu.xmarik.train_manager.events.TCPConnectingErrorEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;

/**
 * TCPClient is a basic TCP client that connects to the server and synchronously waits for
 * incoming data. It is intended to be in separate thread.
 */
public class TCPClient {
    private String serverHost = "localhost";
    private int serverPort = 0;

    private Socket m_socket = null;

    private ConnectThread m_ct = null;
    private TCPWriteThread m_wt = null;
    private TCPReadThread m_rt = null;

    public TCPClient() {
    }

    public void send(String message) throws ConnectException {
        if (m_socket != null && !m_socket.isConnected() || m_wt == null || !m_wt.isAlive())
            throw new ConnectException("Not connected!");

        m_wt.send((message + '\n').getBytes(StandardCharsets.UTF_8));
    }

    public void connect(String host, int port) {
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
        if (m_socket == null)
            return; // already disconnected

        try {
            m_socket.close();
            this.joinSockets();
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_socket = null;
        EventBus.getDefault().post(new TCPDisconnectedEvent(reason));
    }

    private void joinSockets() {
        try {
            m_wt.interrupt();
            m_wt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            m_rt.interrupt();
            m_rt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean connected() {
        return m_socket != null;
    }

    public boolean connecting() {
        return (m_ct != null) && (m_ct.isAlive());
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
}
