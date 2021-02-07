package cz.mendelu.xmarik.train_manager.network;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
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

    private Socket socket = null;

    WriteThread wt = null;
    ReadThread rt = null;

    public TCPClient(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
    }

    public void send(String message) throws ConnectException {
        if (socket != null && !socket.isConnected() || wt == null || !wt.isAlive())
            throw new ConnectException("Not connected!");

        wt.send((message+'\n').getBytes(StandardCharsets.UTF_8));
    }

    public void disconnect() {
        disconnect(true, true);
    }

    public void disconnect(boolean wait_read, boolean wait_write) {
        if (socket != null) {
            try {
                socket.close();

                if (wait_write) {
                    wt.interrupt();
                    try {
                        wt.join();
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (wait_read) {
                    rt.interrupt();
                    try {
                        rt.join();
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                socket = null;
            }
        }
        EventBus.getDefault().post(new TCPDisconnectEvent("Disconnect"));
    }

    public boolean connected() { return socket != null; }

    public void listen(OnMessageReceivedListener listener) {
        if (socket != null) return;
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
                InetAddress serverAddr = InetAddress.getByName(serverIp);
                socket = new Socket(serverAddr, serverPort);

                // disable Nagle's algorithm to make connection low-latency
                socket.setTcpNoDelay(true);
            } catch (Exception e) {
                Log.e("TCP", "Cannot connect to socket", e);
                EventBus.getDefault().post(new TCPDisconnectEvent("Cannot connect to socket"));
                return;
            }

            if (socket == null) {
                Log.e("TCP", "Socket not initialized");
                EventBus.getDefault().post(new TCPDisconnectEvent("Socket not initialized!"));
                return;
            }

            wt = new WriteThread(socket);
            wt.start();
            rt = new ReadThread(socket, m_listener);
            rt.start();

            EventBus.getDefault().post(new ConnectionEstablishedEvent());
        }
    }

    public interface OnMessageReceivedListener {
        public void onMessageReceived(String message);
    }

    public class WriteThread extends Thread {
        private Socket m_socket;
        private final Object m_lock = new Object();
        private ArrayList<byte[]> m_queue = new ArrayList<>();

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
                    } catch(InterruptedException ex) { }

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
        private Socket m_socket;
        private OnMessageReceivedListener m_listener;

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

                    for (int i = 0; i < total_len-last; i++)
                        buffer[i] = buffer[i+last];
                    total_len = total_len - last;
                } catch(IOException e) {
                    disconnect(false, true);
                    return;
                }
            }
        }
    }
}