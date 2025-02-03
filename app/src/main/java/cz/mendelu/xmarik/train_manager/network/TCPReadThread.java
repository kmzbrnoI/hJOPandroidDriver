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
    private final Socket m_socket;

    TCPReadThread(Socket s) {
        m_socket = s;
    }

    void sendDisconnectRequest(String reason) {
        if (!isInterrupted())
            EventBus.getDefault().post(new TCPDisconnectReqEvent(reason));
    }

    public void run() {
        byte[] buffer = new byte[8192];
        int total_len = 0, new_len;

        InputStream str;
        try {
            str = m_socket.getInputStream();
        } catch(IOException e) {
            Log.e("TCP", "Socket IO exception", e);
            sendDisconnectRequest(e.getMessage());
            return;
        }

        while(!isInterrupted() && !m_socket.isClosed()) {
            try {
                new_len = str.read(buffer, total_len, 8192-total_len);
                if (new_len == 0)
                    continue;
                else if (new_len == -1) {
                    sendDisconnectRequest("Reached end of input stream!");
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

                        // This event must be subscribed with threadMode = ThreadMode.MAIN
                        // EventBus does thread synchronization
                        EventBus.getDefault().post(new TCPRawMsgEvent(new String(range, 0, end)));
                        last = i+1;
                    }
                }

                if (total_len - last >= 0)
                    System.arraycopy(buffer, last, buffer, 0, total_len - last);
                total_len = total_len - last;
            } catch(IOException e) {
                sendDisconnectRequest(e.getMessage());
                return;
            }
        }
    }
}
