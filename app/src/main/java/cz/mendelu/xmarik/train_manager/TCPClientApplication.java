package cz.mendelu.xmarik.train_manager;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.net.ConnectException;
import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.AreasEvent;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;
import cz.mendelu.xmarik.train_manager.events.LokEvent;
import cz.mendelu.xmarik.train_manager.events.RequestEvent;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;

/**
 * TCPClientApplication is a singleton, whicch handles connection with the server. It encapsules
 * TCPClient.
 */

public class TCPClientApplication extends Application {
    static TCPClientApplication instance;

    public Server server = null; // TODO should be private and with getter

    public boolean auth = false;
    TCPClient mTcpClient;
    private String tCPAnswer;
    private ArrayList<TCPAnswer> serverResponses;
    private ListenTask listenTask;

    TCPClientApplication() {
        serverResponses = new ArrayList<>();
    }

    public static TCPClientApplication getInstance() {
        if (instance == null) instance = new TCPClientApplication();
        return instance;
    }

    public void connect(Server server) {
        this.server = server;
        mTcpClient = new TCPClient(server.host, server.port);
        listenTask = new ListenTask();
        listenTask.execute();
    }

    public void disconnect() {
        if (mTcpClient != null)
            this.mTcpClient.disconnect();
        if (listenTask != null)
            listenTask.cancel(true);

        this.server = null;
        ServerDb.getInstance().deactivateServer();

        // TODO: notify everyone else
    }

    public void send(String message) {
        if (mTcpClient == null) return;

        try {
            mTcpClient.send(message);
        } catch (ConnectException e) {
            Log.e("TCP", "Cannot send data, disconnecting", e);
            this.disconnect();
        }
    }

    public boolean connected() { return (mTcpClient != null && mTcpClient.connected()); }

    public class ListenTask extends AsyncTask<String, String, TCPClient> {
        @Override
        protected TCPClient doInBackground(String... message) {

            mTcpClient.listen(new TCPClient.OnMessageReceived()  {
                @Override
                public void messageReceived(String message) {
                    publishProgress(message);
                }
            });

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            ArrayList<String> parsed = ParseHelper.parse(values[0], ";", "");

            if (parsed.size() < 2 || !parsed.get(0).equals("-")) return;
            parsed.set(1, parsed.get(1).toUpperCase());

            if (parsed.get(1).equals("HELLO")) {
                EventBus.getDefault().post(new HandShakeEvent(parsed));

            } else if (parsed.get(1).equals("OR-LIST")) {
                EventBus.getDefault().post(new AreasEvent(parsed));

            } else if (parsed.get(1).equals("LOK")) {
                if (parsed.size() < 3) return;
                if (parsed.get(2).equals("G")) {
                    if (parsed.get(3).toUpperCase().equals("AUTH"))
                        EventBus.getDefault().post(new GlobalAuthEvent(parsed));
                    else if (parsed.get(3).toUpperCase().equals("PLEASE-RESP"))
                        EventBus.getDefault().post(new RequestEvent(parsed));
                } else
                    EventBus.getDefault().post(new LokEvent(parsed));
            }

            /*} else if (serverMessage.startsWith("-;LOK;") && !auth) {
                serverMessage = serverMessage.substring("-;LOK;".length());
                String[] tmp = HelpServices.parseHelper(serverMessage);
                if (tmp.length > 2) {
                    String[] lokoData;
                    Train t = ServerDb.getInstance().getActiveServer().getTrain(tmp[0]);
                    if (tmp[1].equals("AUTH")) {
                        switch (tmp[2]) {
                            case "ok":
                                tmp = HelpServices.trainParseHelper(serverMessage);
                                if (tmp.length > 3) {
                                    lokoData = tmp[3].split("\\|");
                                    Train newTrain = new Train(lokoData[0], lokoData[1], lokoData[2], lokoData[3], lokoData[4], lokoData[5], lokoData[6], lokoData[7], lokoData[8]);
                                    newTrain.setAuthorized(true);
                                    newTrain.setTotalManged(false);
                                    if (lokoData.length > 9)
                                        newTrain.setSpeed(Integer.parseInt(lokoData[9]));
                                    if (lokoData.length > 10)
                                        newTrain.setKmhSpeed(Integer.parseInt(lokoData[10]));
                                    if (lokoData.length > 11)
                                        newTrain.setDirection(lokoData[11].equals("0"));
                                    if (lokoData.length > 14)
                                        newTrain.setFunctionNames(lokoData[15]);
                                    ServerDb.getInstance().getActiveServer().addTrain(newTrain);
                                    EventBus.getDefault().post(new TrainReloadEvent(serverMessage));
                                }
                                break;
                            case "total":
                                tmp = HelpServices.trainParseHelper(serverMessage);
                                if (tmp.length > 3) {
                                    lokoData = tmp[3].split("\\|");
                                    Train newTrain = new Train(lokoData[0], lokoData[1], lokoData[2], lokoData[3], lokoData[4], lokoData[5], lokoData[6], lokoData[7], lokoData[8]);
                                    newTrain.setAuthorized(true);
                                    newTrain.setTotalManged(true);
                                    if (lokoData.length > 9)
                                        newTrain.setSpeed(Integer.parseInt(lokoData[9]));
                                    if (lokoData.length > 10)
                                        newTrain.setKmhSpeed(Integer.parseInt(lokoData[10]));
                                    if (lokoData.length > 11)
                                        newTrain.setDirection(lokoData[11].equals("0"));
                                    if (lokoData.length > 14)
                                        newTrain.setFunctionNames(lokoData[15]);
                                    ServerDb.getInstance().getActiveServer().addTrain(newTrain);
                                    EventBus.getDefault().post(new ServerReloadEvent(serverMessage));
                                }
                                break;
                            case "not":
                                t.setAuthorized(false);
                                EventBus.getDefault().post(new ServerReloadEvent(serverMessage));
                                break;
                            case "release":
                                //t.setAuthorized(false);
                                EventBus.getDefault().post(new FreeEvent(serverMessage));
                                break;
                            case "stolen":
                                t.setAuthorized(false);
                                t.setErr("stolen");
                                EventBus.getDefault().post(new ServerReloadEvent(serverMessage));
                                break;
                            default:
                                break;
                        }
                    } else if (tmp[1].equals("TOTAL")) {
                        Train train = ServerDb.getInstance().getActiveServer().getTrain(tmp[0]);
                        train.setTotalManaged(tmp[2].equals("1"));
                        EventBus.getDefault().post(new ServerReloadEvent(serverMessage));
                    } else if (tmp[1].equals("RESP")) {
                        Train train = ServerDb.getInstance().getActiveServer().getTrain(tmp[0]);
                        if (tmp[2].equals("ok")) {
                            train.setKmhSpeed(Integer.parseInt(tmp[3]));
                            train.setErr(null);
                        } else {
                            train.setErr(tmp[3]);
                        }
                        EventBus.getDefault().post(new ServerReloadEvent(serverMessage));
                    } else if (tmp[1].equals("SPD")) {
                        Train train = ServerDb.getInstance().getActiveServer().getTrain(tmp[0]);
                        train.setKmhSpeed(Integer.parseInt(tmp[2]));
                        train.setSpeed(Integer.parseInt(tmp[3]));
                        train.setDirection(tmp[4].equals("1"));
                        train.setErr(null);
                        EventBus.getDefault().post(new ServerReloadEvent(serverMessage));
                    } else if (tmp[1].equals("PLEASE-RESP")) {
                        if (tmp[2].equals("ERR")||tmp[2].equals("err")) {
                            EventBus.getDefault().post(new RequestEvent(tmp[3]));
                        } else EventBus.getDefault().post(new ServerOkEvent("ok"));
                    } else if (tmp[1].equals("F")) {
                        boolean[] func = new boolean[tmp[3].length()];
                        char[] charArray = tmp[3].toCharArray();
                        for (int i = 0; i < charArray.length; i++) {
                            func[i] = charArray[i] != '0';
                        }
                        t.setFunction(func);
                        t.setErr(null);
                        EventBus.getDefault().post(new ServerReloadEvent(serverMessage));
                    } else if (tmp[1].equals("TOTAL")) {
                        t.setTotalManged(tmp[2].equals("1"));
                        EventBus.getDefault().post(new ServerReloadEvent(serverMessage));
                    }
                }
            }*/
        }
    }

    // TODO: subscribe criticcal errors from thread

}
