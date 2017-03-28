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
import cz.mendelu.xmarik.train_manager.events.ErrorEvent;
import cz.mendelu.xmarik.train_manager.events.ReloadEvent;

/**
 * Created by ja on 24. 6. 2016.
 */
public class TCPClient {
    public String SERVERIP = "10.2.0.174"; //your computer IP address
    public int SERVERPORT = 4444;
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    private String serverMessage;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(OnMessageReceived listener, String ip, int port) {
        this.SERVERIP = ip;
        this.SERVERPORT = port;
        mMessageListener = listener;
        Log.e("TCP Client", "port: " + port);
        Log.e("TCP Client", "ip: " + ip);
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(String message) {
        Log.e("", "odeslano:" + message);
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    /**
     * close tcp klient
     */
    public void stopClient() {
        mRun = false;
        if (socket != null) try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String run() {

        mRun = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(SERVERIP);
            Log.e("TCP Client", "C: Connecting...");
            //create a socket to make the connection with the server

            try {
                if (socket == null) socket = new Socket(serverAddr, SERVERPORT);
            } catch (ConnectException e) {
                Log.e("TCP Client", "C: error connection refused");
                EventBus.getDefault().post(new ErrorEvent("error - connection refused"));
                return  "error - connection refused";
            }
            try {
                //send the message to the server
                if (socket != null) {
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    Log.e("TCP Client", "C: Sent.");
                    Log.e("TCP Client", "C: Done.");
                    //receive the message which the server sends back
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //in this while the client listens for the messages sent by the server
                    EventBus.getDefault().post(new ReloadEvent(""));
                    while (mRun) {
                        try {
                            serverMessage = in.readLine();
                        } catch (SocketException e) {
                            EventBus.getDefault().post(new CriticalErrorEvent("Spojení ukončeno"));
                        }
                        if (serverMessage != null && mMessageListener != null) {
                            //call the method messageReceived from MyActivity class
                            Log.e("TCP Client", "C: message received: " + serverMessage);
                            mMessageListener.messageReceived(serverMessage);
                        }
                        serverMessage = null;
                    }
                    Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
                } else {
                    Log.e("TCP", "S: Error - server is unreachable");
                    serverMessage = "error - server is unreachable";
                    mMessageListener.messageReceived(serverMessage);
                }
                //return serverMessage;
            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
                EventBus.getDefault().post(new ErrorEvent("error - server"));
                serverMessage = "error - server";
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                if (socket != null) socket.close();
            }
        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
            serverMessage = "error client";
        }
        return serverMessage;
    }

    public OnMessageReceived getmMessageListener() {
        return mMessageListener;
    }

    public void setmMessageListener(OnMessageReceived mMessageListener) {
        this.mMessageListener = mMessageListener;
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}