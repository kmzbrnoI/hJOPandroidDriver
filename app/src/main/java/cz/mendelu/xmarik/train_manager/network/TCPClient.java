package cz.mendelu.xmarik.train_manager.network;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import cz.mendelu.xmarik.train_manager.events.ConnectionEstablishedEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectEvent;

/**
 * TCPClient is a basic TCP client that connects to the server and synchronously waits for
 * incoming data. It is intended to be in separate thread.
 */
public class TCPClient {
    public String serverIp;
    public int serverPort;

    private Socket m_socket = null;

    private WriteThread m_wt = null;
    private ReadThread m_rt = null;

    public TCPClient(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
    }

    public void send(String message) throws ConnectException {
        if (m_socket != null && !m_socket.isConnected() || m_wt == null || !m_wt.isAlive())
            throw new ConnectException("Not connected!");

        m_wt.send((message+'\n').getBytes(StandardCharsets.UTF_8));
    }

    public void disconnect() {
        disconnect(true, true);
    }

    public void disconnect(boolean wait_read, boolean wait_write) {
        if (m_socket != null) {
            try {
                m_socket.close();

                if (wait_write) {
                    m_wt.interrupt();
                    try {
                        m_wt.join();
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (wait_read) {
                    m_rt.interrupt();
                    try {
                        m_rt.join();
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                m_socket = null;
            }
        }
        EventBus.getDefault().post(new TCPDisconnectEvent("Disconnect"));
    }

    public boolean connected() { return m_socket != null; }

    public void listen(OnMessageReceivedListener listener) {
        if (m_socket != null) return;
        SocketThread st = new SocketThread(serverIp, serverPort, listener);
        st.start();
    }

    public class SocketThread extends Thread {
        public String m_serverIp;
        public int m_serverPort;
        OnMessageReceivedListener m_listener;

        SocketThread(String serverIp, int serverPort, OnMessageReceivedListener listener) {
            m_serverIp = serverIp;
            m_serverPort = serverPort;
            m_listener = listener;
        }

        public void run() {
            try {
                m_socket = new Socket(serverIp, serverPort);
                m_socket.setTcpNoDelay(true); // disable Nagle's algorithm to make connection low-latency
            } catch (Exception e) {
                Log.e("TCP", "Cannot connect to socket", e);
                EventBus.getDefault().post(new TCPDisconnectEvent("Cannot connect to socket"));
                return;
            }

            if (m_socket == null) {
                Log.e("TCP", "Socket not initialized");
                EventBus.getDefault().post(new TCPDisconnectEvent("Socket not initialized!"));
                return;
            }

            m_wt = new WriteThread(m_socket);
            m_wt.start();
            m_rt = new ReadThread(m_socket, m_listener);
            m_rt.start();

            EventBus.getDefault().post(new ConnectionEstablishedEvent());
        }
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    public class WriteThread extends Thread {
        private final Socket m_socket;
        private final Object m_lock = new Object();
        private final ArrayList<byte[]> m_queue = new ArrayList<>();

        WriteThread(Socket s) {
            m_socket = s;
        }

        public void run() {
            OutputStream str;
            try {
                str = m_socket.getOutputStream();
            } catch(IOException e) {
                Log.e("TCP", "Socket IO exception", e);
                disconnect(true, false);
                return;
            }

            synchronized(m_lock) {
                while(!isInterrupted() && !m_socket.isClosed()) {
                    try {
                        m_lock.wait();
                    } catch(InterruptedException ignored) { }

                    for(byte[] data : m_queue) {
                        try {
                            str.write(data);

                        } catch(IOException e) {
                            disconnect(true, false);
                            return;
                        }
                    }
                    m_queue.clear();
                }
            }
        }

        public void send(byte[] data) {
            synchronized(m_lock) {
                m_queue.add(data);
                m_lock.notify();
            }
        }
    }

    public class ReadThread extends Thread {
        private final Socket m_socket;
        private final OnMessageReceivedListener m_listener;

        ReadThread(Socket s, OnMessageReceivedListener listener) {
            m_socket = s;
            m_listener = listener;
        }

        public void run() {
            byte[] buffer = new byte[8192];
            int total_len = 0, new_len;

            InputStream str;
            try {
                str = m_socket.getInputStream();
            } catch(IOException e) {
                Log.e("TCP", "Socket IO exception", e);
                disconnect(false, true);
                return;
            }

            while(!isInterrupted() && !m_socket.isClosed()) {
                try {
                    new_len = str.read(buffer, total_len, 8192-total_len);
                    if (new_len == 0)
                        continue;
                    else if (new_len == -1) {
                        disconnect(false, true);
                        return;
                    }

                    total_len += new_len;

                    int last = 0;
                    for (int i = 0; i < total_len; i++) {
                        if (buffer[i] == '\n') {
                            byte[] range = Arrays.copyOfRange(buffer, last, i);

                            int end;
                            if (i > 0 && buffer[i-1] == '\r')
                                end = i-1 - last;
                            else
                                end = i - last;

                            m_listener.onMessageReceived(new String(range, 0, end));
                            last = i+1;
                        }
                    }

                    if (total_len - last >= 0)
                        System.arraycopy(buffer, last, buffer, 0, total_len - last);
                    total_len = total_len - last;
                } catch(IOException e) {
                    disconnect(false, true);
                    return;
                }
            }
        }
    }
}
