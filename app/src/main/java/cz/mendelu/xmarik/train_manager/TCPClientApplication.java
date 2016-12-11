package cz.mendelu.xmarik.train_manager;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.AreasEvent;
import cz.mendelu.xmarik.train_manager.events.FreeEvent;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;
import cz.mendelu.xmarik.train_manager.events.RefuseEvent;
import cz.mendelu.xmarik.train_manager.events.ReloadEvent;
import cz.mendelu.xmarik.train_manager.events.TrainReloadEvent;

/**
 * Created by ja on 9. 10. 2016.
 */

public class TCPClientApplication extends Application {

    static TCPClientApplication instance;
    public boolean auth = false;
    TCPClient mTcpClient;
    Server server;
    private String tCPAnswer;
    private ArrayList<TCPAnswer> serverResponses;

    TCPClientApplication() {
        serverResponses = new ArrayList<>();
    }

    public static TCPClientApplication getInstance() {
        if (instance == null) {
            instance = new TCPClientApplication();
        }

        return instance;
    }

    public static TCPClientApplication startNewServer() {
        instance = new TCPClientApplication();
        return instance;
    }

    public TCPClient getClient() {
        return this.mTcpClient;
    }

    public void setClient(TCPClient client) {
        this.mTcpClient = client;
    }

    public void customAppMethod() {
        // Custom application method
    }

    public void connect(Server server) {
        this.server = server;
    }

    public void start() {
        new connectTask().execute();
    }

    public void sendToServer(String message) {
        Log.e("", "odeslano:" + message);
        //sends the message to the server
        if (mTcpClient != null) {
            mTcpClient.sendMessage(message);
        }
    }

    public class connectTask extends AsyncTask<String, String, TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            }, server.ipAdr, server.port);
            String answer = mTcpClient.run();

            if (answer != null && answer.equals("server is unreachable")) {
                tCPAnswer = "client error, please check your connection";
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            String serverMessage = values[0];
            //parsovat odpoved od serveru je ve values 0
            Log.e("", "zprava :" + serverMessage);
            if (serverMessage.startsWith("-;MOD-CAS") || serverMessage.startsWith("-;DCC")) {

            } else if (serverMessage.startsWith("-;HELLO;")) {
                EventBus.getDefault().post(new HandShakeEvent(serverMessage));
            } else if (serverMessage.startsWith("-;LOK;G;AUTH;ok;")) {
                EventBus.getDefault().post(new HandShakeEvent(serverMessage));
            } else if (serverMessage.startsWith("-;OR-LIST;")) {
                EventBus.getDefault().post(new AreasEvent(serverMessage));
            } else if (serverMessage.startsWith("-;LOK;") && !auth) {
                serverMessage = serverMessage.substring("-;LOK;".length());
                String[] tmp = HelpServices.parseHelper(serverMessage);
                if (tmp.length > 2) {
                    String[] lokoData;
                    Train t = ServerList.getInstance().getActiveServer().getTrain(tmp[0]);
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
                                    ServerList.getInstance().getActiveServer().addTrain(newTrain);
                                    EventBus.getDefault().post(new TrainReloadEvent(serverMessage));
                                }
                                break;
                            case "total":
                                //TODO pri testovani spadlo, prislo
                                /*4310;AUTH;total;{413.0|Mendelova univerzita|413.0|{}|4310|0|804310|0|00000000000000000000000000000|0|0|1|Bs|||{{světla};;{zvuk};;;{píšťala dlouhá};{zvon};{spřáhlo};;;;;;;;;;;;;;;;;;;;;;}|}'
                                * received: -;LOK;4310;F;0-28;10000000000000000000000000000;
                                * */
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
                                    ServerList.getInstance().getActiveServer().addTrain(newTrain);
                                    EventBus.getDefault().post(new ReloadEvent(serverMessage));
                                }
                                break;
                            case "not":
                                t.setAuthorized(false);
                                EventBus.getDefault().post(new ReloadEvent(serverMessage));
                                break;
                            case "release":
                                t.setAuthorized(false);
                                EventBus.getDefault().post(new FreeEvent(serverMessage));
                                break;
                            case "stolen":
                                t.setAuthorized(false);
                                t.setErr("stolen");
                                EventBus.getDefault().post(new ReloadEvent(serverMessage));
                                break;
                            default:
                                break;
                        }
                    } else if (tmp[1].equals("TOTAL")) {
                        Train train = ServerList.getInstance().getActiveServer().getTrain(tmp[0]);
                        train.setTotalManaged(tmp[2].equals("1"));
                        EventBus.getDefault().post(new ReloadEvent(serverMessage));
                    } else if (tmp[1].equals("RESP")) {
                        Train train = ServerList.getInstance().getActiveServer().getTrain(tmp[0]);
                        if (tmp[2].equals("ok")) {
                            train.setKmhSpeed(Integer.parseInt(tmp[3]));
                            train.setErr(null);
                        } else {
                            train.setErr(tmp[3]);
                        }
                        EventBus.getDefault().post(new ReloadEvent(serverMessage));
                    } else if (tmp[1].equals("SPD")) {
                        Train train = ServerList.getInstance().getActiveServer().getTrain(tmp[0]);
                        train.setKmhSpeed(Integer.parseInt(tmp[2]));
                        train.setSpeed(Integer.parseInt(tmp[3]));
                        train.setDirection(tmp[4].equals("1"));
                        train.setErr(null);
                        EventBus.getDefault().post(new ReloadEvent(serverMessage));
                    } else if (tmp[1].equals("PLEASE-RESP")) {
                        if (tmp[2].equals("ERR"))
                            EventBus.getDefault().post(new RefuseEvent(tmp[3]));
                    } else if (tmp[1].equals("F")) {
                        String borders[] = tmp[2].split("-");
                        int left = Integer.parseInt(borders[1]);
                        int right = Integer.parseInt(borders[2]);
                        boolean[] func = new boolean[tmp[3].length()];
                        char[] charArray = tmp[3].toCharArray();
                        for (int i = 0; i < charArray.length; i++) {
                            func[i] = charArray[i] != '0';
                        }
                        t.setFunction(func);
                        t.setErr(null);
                        EventBus.getDefault().post(new ReloadEvent(serverMessage));
                    } else if (tmp[1].equals("TOTAL")) {
                        t.setTotalManged(tmp[2].equals("1"));
                        EventBus.getDefault().post(new ReloadEvent(serverMessage));
                    }
                }
            }
        }
    }

}
