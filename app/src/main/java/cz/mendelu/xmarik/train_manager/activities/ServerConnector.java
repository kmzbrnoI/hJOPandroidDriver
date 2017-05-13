package cz.mendelu.xmarik.train_manager.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.AreasParsedEvent;
import cz.mendelu.xmarik.train_manager.helpers.HashHelper;
import cz.mendelu.xmarik.train_manager.adapters.MyCustomAdapter;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.ConnectionEstablishedEvent;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;
import cz.mendelu.xmarik.train_manager.network.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectEvent;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;

public class ServerConnector extends Activity {
    private static String[] messges = {};//dodelat vsechno retezce co budou treba
    private static int i = 0;
    String user = null;
    String passwd = null;
    boolean ok;
    private ListView mList;
    private ArrayList<String> arrayList;
    private MyCustomAdapter mAdapter;
    private Button send;
    private ServerConnector classObject;
    private Server server;
    private boolean tcpOK = false;
    private ProgressBar progressBar;

    public static final MonitorObject  monitor = new MonitorObject();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_connector);
    }

    @Override
    public void onBackPressed() {
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
        TCPClientApplication.getInstance().disconnect();
        super.onBackPressed();
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
            this.server.username = name;
            this.server.password = tmpPass;
            ServerDb.getInstance().setPassword(server);
        }
        Log.v("", "user a heslo:" + name + " \n" + tmpPass);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        i = 0;
        tcpOK = false;
        if (TCPClientApplication.getInstance() != null)
            TCPClientApplication.getInstance().disconnect();
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        startMethod();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
    }

    public void changeUserData(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_user);
        dialog.setTitle(getString(R.string.login_enter));
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
                passwd = HashHelper.hashPasswd(mPasswd.getText().toString());
                setData(user, passwd, savebox.isChecked());
                messges[1] = "-;LOK;G;AUTH;{" + user + "};" + passwd;
                //initialize();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Subscribe
    public void onEvent(AreasParsedEvent event) {
        arrayList.add(getString(R.string.sc_done));
        mAdapter.notifyDataSetChanged();

        Toast.makeText(getApplicationContext(),
                R.string.conn_connected, Toast.LENGTH_LONG)
                .show();

        progressBar.setVisibility(View.GONE);

        Intent intent = new Intent(this, TrainRequest.class);
        startActivity(intent);
    }

    @Subscribe
    public void onEvent(HandShakeEvent event) {
        if ((event.getParsed().size() < 3) || (!event.getParsed().get(2).equals("1.0")))
            arrayList.add(getString(R.string.sc_version_warning));
        else
            arrayList.add(getString(R.string.sc_connection_ok));

        // TODO: check username and password non-existence

        arrayList.add(getString(R.string.sc_authorizing));
        mAdapter.notifyDataSetChanged();

        TCPClientApplication.getInstance().send("-;LOK;G;AUTH;{" + TCPClientApplication.getInstance().server.username + "};" +
            TCPClientApplication.getInstance().server.password);
    }

    @Subscribe
    public void onEvent(GlobalAuthEvent event) {
        if (event.getParsed().get(4).toUpperCase().equals("OK")) {
            arrayList.add(getString(R.string.sc_auth_ok));
            arrayList.add(getString(R.string.sc_getting_ors));
            TCPClientApplication.getInstance().send("-;OR-LIST");
        } else {
            arrayList.add(getString(R.string.sc_auth_err));

            // TODO: handle general disconnect
        }
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onEvent(TCPDisconnectEvent event) {
        // TODO
    }

    private void raiseErrorState(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                i = 0;
                send.setClickable(true);
                send.setText(R.string.gl_repeat);
                arrayList.add(error);
                mAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                showDialog("Error", error);
            }
        });
    }

    private void startMethod() {
        if(!EventBus.getDefault().isRegistered(this))EventBus.getDefault().register(this);
        Bundle extras = getIntent().getExtras();
        TCPClientApplication tcp = TCPClientApplication.getInstance();

        if (tcp.connected())
            tcp.disconnect();

        if ( extras != null ) {
            String value = extras.getString("server");
            String[] tmp = value.split("\t");
            server = ServerDb.getInstance().getServer(tmp[0]);
            tcp.auth = true;

            try {
                tcp.connect(server);
            } catch (Exception e) {
                // TODO
                Log.e("TCP", "Connecting", e);
            }

        } else if (this.server != null) {
            tcp.auth = true;
            try {
                tcp.connect(server);
            } catch (Exception e) {
                // TODO
                Log.e("TCP", "Connecting", e);
            }
        } else finish();

        arrayList = new ArrayList<>();

        send = (Button) findViewById(R.id.send_button);
        progressBar = (ProgressBar) findViewById(R.id.serverLoadBar);
        progressBar.setVisibility(View.VISIBLE); //relate the listView from java to the one created in xml
        mList = (ListView) findViewById(R.id.list);
        mAdapter = new MyCustomAdapter(this, arrayList);
        mList.setAdapter(mAdapter);


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //initialize();
            }

        });

        send.setClickable(false);
        send.setText(getString(R.string.sc_authorizing));
        arrayList.clear();
        arrayList.add(getString(R.string.sc_connecting));
        mAdapter.notifyDataSetChanged();

    }

    @Subscribe
    public void onEvent(ConnectionEstablishedEvent event) {
        // connection established -> begin handshake
        TCPClientApplication.getInstance().send("-;HELLO;1.0");
    }

    public static class MonitorObject{
    }

    public void showDialog(String title, String chyba) {
        if (chyba != null && chyba.startsWith(";"))
            chyba = chyba.substring(1);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_user);
        dialog.setTitle(title);
        //set dialog component
        final EditText mName = (EditText) dialog.findViewById(R.id.dialogName);
        final EditText mPasswd = (EditText) dialog.findViewById(R.id.dialogPasswd);
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
        final CheckBox save = (CheckBox) dialog.findViewById(R.id.dialogSaveData);
        TextView error = (TextView) dialog.findViewById(R.id.errorMessage);
        if(chyba != null) {
            error.setText(chyba);
        } else {
            error.setText("");
        }
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
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                user = mName.getText().toString();
                passwd = HashHelper.hashPasswd(mPasswd.getText().toString());
                if (save.isChecked()) {
                    setData(user, passwd, true);
                } else setData(user, passwd, false);
                messges[1] = "-;LOK;G;AUTH;{" + user + "};" + passwd;
            }
        });
        dialog.show();
    }

}