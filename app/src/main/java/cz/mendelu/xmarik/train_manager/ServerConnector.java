package cz.mendelu.xmarik.train_manager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.AreasEvent;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;
import cz.mendelu.xmarik.train_manager.events.ReloadEvent;


public class ServerConnector extends Activity {
    private static String[] messges = {};//dodelat vsechno retezce co budou treba
    private static int i = 0;
    String user = null;
    String passwd = null;
    boolean ok;
    private ListView mList;
    private ArrayList<String> arrayList;
    private MyCustomAdapter mAdapter;
    private TCPClient mTcpClient;
    private Button send;
    private ServerConnector classObject;
    private Server server;
    private boolean faildSender = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        classObject = this;
        EventBus.getDefault().register(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_connector);
        Bundle extras = getIntent().getExtras();
        TCPClientApplication tcp = TCPClientApplication.startNewServer();
        messges = new String[3];
        if (extras != null) {
            String value = extras.getString("server");
            String[] tmp = value.split("\t");
            server = ServerList.getInstance().getServer(tmp[0]);
            Log.e("", "server" + server.getDnsName());
            Log.e("services", "ulozene jmeno a heslo: " + server.getUserName() + "  " + server.getUserPassword());
            tcp.server = server;
            tcp.auth = true;
            tcp.start();
        } else finish();
        messges[0] = "-;HELLO;1.0\n";
        if (server.getUserName() != null && server.getUserPassword() != null) {
            user = server.getUserName();
            passwd = server.getUserPassword();
            messges[1] = "-;LOK;G;AUTH;{" + user + "};" + passwd + "\n";
        } else {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.user_dialog);
            dialog.setTitle("Title...");
            //set dialog component
            final EditText mName = (EditText) dialog.findViewById(R.id.dialogName);
            final EditText mPasswd = (EditText) dialog.findViewById(R.id.dialogPasswd);
            Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
            final CheckBox save = (CheckBox) dialog.findViewById(R.id.dialogSaveData);
            mName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (mName.isFocused()) mName.setText("");
                }
            });
            mPasswd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (mPasswd.isFocused()) mPasswd.setText("");
                }
            });
            // if button is clicked, close the custom dialog
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    user = mName.getText().toString();
                    passwd = HelpServices.hashPasswd(mPasswd.getText().toString());
                    if (save.isChecked()) {
                        setData(user, passwd, true);
                    } else setData(user, passwd, false);
                    messges[1] = "-;LOK;G;AUTH;{" + user + "};" + passwd + "\n";
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
        messges[2] = "-;OR-LIST;" + "\n";
        arrayList = new ArrayList<>();
        send = (Button) findViewById(R.id.send_button);
        //relate the listView from java to the one created in xml
        mList = (ListView) findViewById(R.id.list);
        mAdapter = new MyCustomAdapter(this, arrayList);
        mList.setAdapter(mAdapter);
        //new connectTask().execute("");
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initialize();
                sendNext();
            }

        });
        //TODO podminka ze settings
        initialize();
    }

    public void setData(String name, String tmpPass, boolean save) {
        name = name.replaceAll("\n", "");
        tmpPass = tmpPass.replaceAll("\n", "");
        name = name.replaceAll("\\{", "");
        tmpPass = tmpPass.replaceAll("\\{", "");
        name = name.replaceAll("\\}", "");
        tmpPass = tmpPass.replaceAll("\\}", "");
        this.user = name;
        this.passwd = tmpPass;
        if (save) {
            this.server.setUserName(name);
            this.server.setUserPassword(tmpPass);
            ServerList.getInstance().setPassword(server);
        }
        Log.e("", "user a heslo:" + name + " \n" + tmpPass);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public String getMessage() {
        if (messges.length > i) {
            String tmp = messges[i];
            if (i == 0) {
                arrayList.add("Navazuji komunikaci");
            } else if (i == 1) {
                arrayList.add("Autorizuji");
            } else arrayList.add("Získávám oblasti řízení");
            i++;
            return tmp;
        }
        return null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        /*TCPClientApplication tcp = TCPClientApplication.startNewServer();
        tcp.server = server;
        tcp.auth = true;
        tcp.start();*/
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    private void sendNext() {
        String message = getMessage();
        //sends the message to the server
        Log.e("", "Send pokus odeslat:" + message);
        if (TCPClientApplication.getInstance().getClient() != null && message != null) {
            TCPClientApplication.getInstance().getClient().sendMessage(message);
            Log.e("", "Send odeslano:" + message);
        } else faildSender = true;
        //refresh the list
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onEvent(ReloadEvent event) {
        Log.e("", "pokus odeslat eventem:" + "-;HELLO;1.0");
        if(!faildSender) {
            Log.e("", "event odeslal:" + "-;HELLO;1.0");
            TCPClientApplication.getInstance().getClient().sendMessage("-;HELLO;1.0\n");
        }
    }

    public void changeUserData(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.user_dialog);
        dialog.setTitle("Title...");
        //set dialog component
        final EditText mName = (EditText) dialog.findViewById(R.id.dialogName);
        final EditText mPasswd = (EditText) dialog.findViewById(R.id.dialogPasswd);
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
        final CheckBox savebox = (CheckBox) dialog.findViewById(R.id.dialogSaveData);

        mName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (mName.isFocused()) mName.setText("");
            }
        });
        mPasswd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (mPasswd.isFocused()) mPasswd.setText("");
            }
        });
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                user = mName.getText().toString();
                passwd = HelpServices.hashPasswd(mPasswd.getText().toString());
                setData(user, passwd, savebox.isChecked());
                messges[1] = "-;LOK;G;AUTH;{" + user + "};" + passwd + "\n";
                initialize();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void initialize() {
        send.setClickable(false);
        send.setText("autorizuji");
        arrayList.clear();
        arrayList.add("připojuji k serveru");
        mAdapter.notifyDataSetChanged();
        sendNext();
    }

    @Subscribe
    public void onEvent(AreasEvent event) {
        // your implementation
        Log.e("", "Area event : " + event.getMessage());
        addControlAreas(event.getMessage().substring("-;OR-LIST;".length()));
        arrayList.add("Data načteny, aktivace serveru dokončena");
        Intent returnIntent = new Intent();
        //TODO dodelat nejakou chybu
        server.setTcpClient(TCPClientApplication.getInstance().getClient());
        server.setActive(true);
        ServerList.getInstance().setActive(server);
        TCPClientApplication.getInstance().auth = false;
        if (ok) {
            Toast.makeText(getApplicationContext(),
                    "Server authorized and connected", Toast.LENGTH_LONG)
                    .show();
            Intent intent = new Intent(this, Trains_box.class);
            startActivity(intent);
        } else {
            returnIntent.putExtra("result", "authorization failed");
            setResult(RESULT_CANCELED, returnIntent);
            finish();
        }
    }

    @Subscribe
    public void onEvent(HandShakeEvent event) {
        // your implementation
        Log.e("", "Hand shake : " + event.getMessage());
        if (event.getMessage().startsWith("-;HELLO;")) {
            arrayList.add("komunikace navázána");
            sendNext();
            if (!event.getMessage().substring("-;HELLO;".length()).equals("1.0")) {
                arrayList.add("Server využívá jiný komunikační protokol než 1.0, v komunikaci může docházet k chybám");
            }
        } else if (event.getMessage().startsWith("-;LOK;G;AUTH;ok;")) {
            ok = true;
            arrayList.add("Strojvůdce autorizován");
            sendNext();
        } else if (event.getMessage().startsWith("-;LOK;G;AUTH;")) {
            ok = false;
            raiseErrorState("autorizace se nezdarila");
        } else raiseErrorState("handshake selhal");
    }

    private void addControlAreas(String data) {
        String[] tmp = data.split("\\[*\\]");
        server.areas = new ArrayList<ControlArea>();
        for (String s : tmp) {
            s = s.replace("[", "");
            s = s.replace("]", "");
            s = s.replace("];", "");
            String[] area = s.split(",");
            if (area.length > 1)
                Log.e("", "area : " + area.toString());
            server.addArea(new ControlArea(area[0], area[1]));
        }
    }

    private void raiseErrorState(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                i = 0;
                send.setClickable(true);
                send.setText("opakovat");
                arrayList.add(error);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

}