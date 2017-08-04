package cz.mendelu.xmarik.train_manager.network;

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

        if (out.checkError()) {
            Log.e("TCP", "Socket send error, closing");
            disconnect();
        }
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
                in = null;
                out = null;
            }
        }
        EventBus.getDefault().post(new TCPDisconnectEvent("Disconnect"));
    }

    public boolean connected() { return socket != null; }

    public void listen(OnMessageReceived listener) {
        String serverMessage = null;
        mRun = true;

        if (socket != null) return;

        try {
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddr, serverPort);

            // disable Nagle's algorithm to make connection low-latency
            socket.setTcpNoDelay(true);
        } catch (Exception e) {
            EventBus.getDefault().post(new TCPDisconnectEvent("Cannot connect to socket"));
        }

        if (socket == null) {
            EventBus.getDefault().post(new TCPDisconnectEvent("Socket not initialized!"));
            return;
        }

        try {
            //send the message to the server
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            EventBus.getDefault().post(new ConnectionEstablishedEvent());

            while (mRun) {
                serverMessage = in.readLine();

                try {
                    if (serverMessage != null && listener != null)
                        listener.messageReceived(serverMessage);
                } catch (Exception e) {
                    Log.e("TCP", "Socket message error", e);
                }

            }

        } catch (Exception e) {
            Log.e("TCP", "Socket general error", e);
            EventBus.getDefault().post(new TCPDisconnectEvent("Socket general error"));

        } finally {
            //the socket must be closed. It is not possible to reconnect to this socket
            // after it is closed, which means a new socket instance has to be created.
            disconnect();
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}