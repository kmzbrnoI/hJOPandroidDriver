package cz.mendelu.xmarik.train_manager.network;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.TCPDisconnectReqEvent;

public class TCPWriteThread extends Thread {
    private final Socket m_socket;
    private final Object m_lock = new Object();
    private final ArrayList<byte[]> m_queue = new ArrayList<>();

    TCPWriteThread(Socket s) {
        m_socket = s;
    }

    void sendDisconnectRequest(String reason) {
        if (!isInterrupted())
            EventBus.getDefault().post(new TCPDisconnectReqEvent(reason));
    }

    public void run() {
        OutputStream str;
        try {
            str = m_socket.getOutputStream();
        } catch (IOException e) {
            Log.e("TCP", "WriteThread::run OutputStream", e);
            sendDisconnectRequest(e.getMessage());
            return;
        }

        synchronized (m_lock) {
            while (!isInterrupted() && !m_socket.isClosed()) {
                try {
                    m_lock.wait();
                } catch (InterruptedException ignored) {
                }

                for (byte[] data : m_queue) {
                    try {
                        str.write(data);
                    } catch (IOException e) {
                        sendDisconnectRequest(e.getMessage());
                        return;
                    }
                }
                m_queue.clear();
            }
        }
    }

    public void send(byte[] data) {
        synchronized (m_lock) {
            m_queue.add(data);
            m_lock.notify();
        }
    }
}
