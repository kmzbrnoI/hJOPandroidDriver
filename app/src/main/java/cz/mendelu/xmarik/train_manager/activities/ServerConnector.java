package cz.mendelu.xmarik.train_manager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

import cz.mendelu.xmarik.train_manager.events.AreasParsedEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectEvent;
import cz.mendelu.xmarik.train_manager.helpers.HashHelper;
import cz.mendelu.xmarik.train_manager.adapters.TextViewAdapter;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.ConnectionEstablishedEvent;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;
import cz.mendelu.xmarik.train_manager.network.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;

public class ServerConnector extends AppCompatActivity {
    private ArrayList<String> arrayList;
    private TextViewAdapter mAdapter;
    private ProgressBar progressBar;

    public static final String[] serverSupportedVersions = {"1.0", "1.1"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_server_connector);
        super.onCreate(savedInstanceState);

        arrayList = new ArrayList<>();
        progressBar = findViewById(R.id.serverLoadBar);
        progressBar.setVisibility(View.VISIBLE);
        ListView mList = findViewById(R.id.list);
        mAdapter = new TextViewAdapter(this, arrayList);
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (TCPClientApplication.getInstance().connected())
            TCPClientApplication.getInstance().disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        start();
    }

    public void editLogin(String message) {
        View dialogView = this.getLayoutInflater().inflate(R.layout.dialog_user, null);
        final EditText mName = dialogView.findViewById(R.id.dialogName);
        final EditText mPasswd = dialogView.findViewById(R.id.dialogPasswd);
        final CheckBox savebox = dialogView.findViewById(R.id.dialogSaveData);

        mName.setText(TCPClientApplication.getInstance().server.username);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    TCPClientApplication.getInstance().server.username = mName.getText().toString().trim();
                    TCPClientApplication.getInstance().server.password = HashHelper.hashPasswd(mPasswd.getText().toString());

                    if (savebox.isChecked()) {
                        if (ServerDb.instance.isStoredServer(TCPClientApplication.getInstance().server.host,
                                TCPClientApplication.getInstance().server.port))
                            ServerDb.instance.transferLoginToSaved(TCPClientApplication.getInstance().server);
                        else
                            ServerDb.instance.addStoredServer(TCPClientApplication.getInstance().server);
                    }

                    TCPClientApplication.getInstance().send("-;LOK;G;AUTH;{" +
                            TCPClientApplication.getInstance().server.username + "};" +
                            TCPClientApplication.getInstance().server.password);

                    arrayList.add(getString(R.string.sc_authorizing));
                    mAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.VISIBLE);
                })
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AreasParsedEvent event) {
        arrayList.add(getString(R.string.sc_done));
        mAdapter.notifyDataSetChanged();

        Toast.makeText(getApplicationContext(),
                R.string.conn_connected, Toast.LENGTH_LONG)
                .show();

        progressBar.setVisibility(View.GONE);

        Intent intent = new Intent(this, TrainRequest.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(HandShakeEvent event) {
        if ((event.getParsed().size() < 3) || (!Arrays.asList(serverSupportedVersions).contains(event.getParsed().get(2))))
            arrayList.add(getString(R.string.sc_version_warning));
        else
            arrayList.add(getString(R.string.sc_connection_ok));

        mAdapter.notifyDataSetChanged();

        if (TCPClientApplication.getInstance().server.username.isEmpty() ||
                TCPClientApplication.getInstance().server.password.isEmpty()) {
            arrayList.add(getString(R.string.sc_auth_wait));
            progressBar.setVisibility(View.GONE);
            editLogin(getString(R.string.login_enter));
        } else {
            arrayList.add(getString(R.string.sc_authorizing));
            TCPClientApplication.getInstance().send("-;LOK;G;AUTH;{" +
                    TCPClientApplication.getInstance().server.username + "};" +
                    TCPClientApplication.getInstance().server.password);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(GlobalAuthEvent event) {
        if (event.getParsed().get(4).equalsIgnoreCase("OK")) {
            arrayList.add(getString(R.string.sc_auth_ok));
            arrayList.add(getString(R.string.sc_getting_ors));
            TCPClientApplication.getInstance().send("-;OR-LIST");
        } else {
            arrayList.add(getString(R.string.sc_auth_err));
            if (event.getParsed().size() >= 6)
                arrayList.add(event.getParsed().get(5));
            progressBar.setVisibility(View.GONE);
            if (event.getParsed().size() >= 6)
                editLogin(event.getParsed().get(5));
            else
                editLogin(getString(R.string.sc_auth_err));
        }
        mAdapter.notifyDataSetChanged();
    }

    private void start() {
        Bundle extras = getIntent().getExtras();
        TCPClientApplication tcp = TCPClientApplication.getInstance();
        Server server;

        if (tcp.connected())
            tcp.disconnect();

        if (extras != null) {
            String type = extras.getString("serverType");
            int id = extras.getInt("serverId");
            if (type.equals("stored"))
                server = ServerDb.instance.stored.get(id);
            else
                server = ServerDb.instance.found.get(id);

            try {
                tcp.connect(server);
            } catch (Exception e) {
                Log.e("TCP", "Connecting", e);
                arrayList.add(e.toString());
                mAdapter.notifyDataSetChanged();
            }

        } else finish();

        arrayList.clear();
        arrayList.add(getString(R.string.sc_connecting));
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConnectionEstablishedEvent event) {
        // connection established -> begin handshake
        TCPClientApplication.getInstance().send("-;HELLO;1.1");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPDisconnectEvent event) {
        arrayList.add(getString(R.string.disconnected) + "\n(" + event.getError() + ")");
        progressBar.setVisibility(View.GONE);
        mAdapter.notifyDataSetChanged();
    }
}