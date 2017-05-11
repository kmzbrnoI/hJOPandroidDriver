package cz.mendelu.xmarik.train_manager;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import cz.mendelu.xmarik.train_manager.events.CriticalErrorEvent;
import cz.mendelu.xmarik.train_manager.events.TCPErrorEvent;
import cz.mendelu.xmarik.train_manager.events.ServerReloadEvent;

/**
 * TCPClient is a basic TCP client that connects to the server and synchronously waits for
 * incoming data. It is intended to be in separate thread.
 */
public class TCPClient {
    public String serverIp;
    public int serverPort;

    private Socket socket = null;
    private PrintWriter out;
    private BufferedReader in;
    private boolean mRun = false;

    public TCPClient(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
    }

    public void send(String message) throws ConnectException {
        if (out == null)
            throw new ConnectException("Not connected!");

        out.println(message);
        out.flush();
    }

    public void disconnect() {
        mRun = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                socket = null;
            }
        }
    }

    public boolean connected() { return socket != null; }

    public void listen(OnMessageReceived listener) {
        String serverMessage = null;
        mRun = true;

        if (socket != null) return;

        try {
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddr, serverPort);
        } catch (Exception e) {
            EventBus.getDefault().post(new TCPErrorEvent("Cannot connect to socket"));
        }

        if (socket == null) return;

        try {
            //send the message to the server
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (mRun) {
                try {
                    serverMessage = in.readLine();
                } catch (SocketException e) {
                    EventBus.getDefault().post(new TCPErrorEvent("Socket exception!"));
                }

                if (serverMessage != null && listener != null) {
                    listener.messageReceived(serverMessage);
                }
            }

        } catch (Exception e) {
            Log.e("TCP", "Socket general error", e);
            EventBus.getDefault().post(new TCPErrorEvent("Socket general error"));

        } finally {
            //the socket must be closed. It is not possible to reconnect to this socket
            // after it is closed, which means a new socket instance has to be created.
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e("TCP", "Socket close exception", e);
                }
            }
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}