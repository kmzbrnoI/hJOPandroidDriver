package cz.mendelu.xmarik.train_manager.network;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

import cz.mendelu.xmarik.train_manager.events.TCPDisconnectReqEvent;
import cz.mendelu.xmarik.train_manager.events.TCPRawMsgEvent;

public class TCPReadThread extends Thread {
    static final int READ_BUF_SIZE = 8192;
    private final Socket m_socket;

    TCPReadThread(Socket s) {
        m_socket = s;
    }

    void sendDisconnectRequest(String reason) {
        if (!isInterrupted())
            EventBus.getDefault().post(new TCPDisconnectReqEvent(reason));
    }

    public void run() {
        InputStream str;
        try {
            str = m_socket.getInputStream();
        } catch(IOException e) {
            Log.e("TCP", "Socket IO exception", e);
            sendDisconnectRequest(e.getMessage());
            return;
        }

        byte[] buffer = new byte[READ_BUF_SIZE];
        int pending_len = 0;

        while ((!isInterrupted()) && (!m_socket.isClosed())) {
            try {
                int read_len = str.read(buffer, pending_len, READ_BUF_SIZE- pending_len);
                if (read_len == 0) {
                    continue;
                } else if (read_len == -1) {
                    sendDisconnectRequest("Reached end of input stream!");
                    return;
                }

                pending_len += read_len;

                int message_begin = 0;
                for (int i = 0; i < pending_len; i++) {
                    if (buffer[i] == '\n') {
                        byte[] message = Arrays.copyOfRange(buffer, message_begin, i);
                        int end = ((i > 0) && (buffer[i-1] == '\r')) ? (i-message_begin-1) : (i-message_begin);

                        // This event must be subscribed with threadMode = ThreadMode.MAIN
                        // EventBus does thread synchronization
                        EventBus.getDefault().post(new TCPRawMsgEvent(new String(message, 0, end)));
                        message_begin = i+1;
                    }
                }

                if ((pending_len-message_begin) >= 0)
                    System.arraycopy(buffer, message_begin, buffer, 0, pending_len - message_begin);
                pending_len = pending_len - message_begin;
            } catch (IOException e) {
                sendDisconnectRequest(e.getMessage());
            }
        }
    }
}
