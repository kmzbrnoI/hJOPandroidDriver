package cz.mendelu.xmarik.train_manager.activities;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

import cz.mendelu.xmarik.train_manager.events.AreasParsedEvent;
import cz.mendelu.xmarik.train_manager.events.TCPConnectingErrorEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;
import cz.mendelu.xmarik.train_manager.helpers.HashHelper;
import cz.mendelu.xmarik.train_manager.adapters.TextViewAdapter;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.ConnectionEstablishedEvent;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;
import cz.mendelu.xmarik.train_manager.network.TCPClient;
import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;

public class ServerConnector extends AppCompatActivity {
    private ArrayList<String> messages;
    private TextViewAdapter messagesAdapter;
    private ProgressBar progressBar;

    public static final String[] serverSupportedVersions = {"1.0", "1.1"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.dialog_server_connector);
        super.onCreate(savedInstanceState);

        messages = new ArrayList<>();
        progressBar = findViewById(R.id.serverLoadBar);
        ListView mList = findViewById(R.id.list);
        messagesAdapter = new TextViewAdapter(this, messages);
        mList.setAdapter(messagesAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (TCPClient.getInstance().connected())
            TCPClient.getInstance().disconnect(getString(R.string.sc_disconnected_user_cancelled));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        start();
    }

    public void editLogin(String message) {
        View dialogView = this.getLayoutInflater().inflate(R.layout.dialog_user, null);
        final EditText mName = dialogView.findViewById(R.id.dialogName);
        final EditText mPasswd = dialogView.findViewById(R.id.dialogPasswd);
        final CheckBox savebox = dialogView.findViewById(R.id.dialogSaveData);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        mName.setText(TCPClient.getInstance().server.username);
        savebox.setVisibility(View.VISIBLE);
        savebox.setChecked(preferences.getBoolean("RememberPasswordDefault", true));

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    TCPClient.getInstance().server.username = mName.getText().toString().trim();
                    TCPClient.getInstance().server.password = HashHelper.hashPasswd(mPasswd.getText().toString());

                    if (savebox.isChecked()) {
                        if (ServerDb.instance.isStoredServer(TCPClient.getInstance().server))
                            ServerDb.instance.transferLoginToStored(TCPClient.getInstance().server);
                        else
                            ServerDb.instance.addStoredServer(TCPClient.getInstance().server);
                    }

                    TCPClient.getInstance().send("-;LOK;G;AUTH;{" +
                            TCPClient.getInstance().server.username + "};" +
                            TCPClient.getInstance().server.password);

                    this.addMessage(getString(R.string.sc_authorizing));
                    progressBar.setVisibility(View.VISIBLE);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    TCPClient.getInstance().disconnect(getString(R.string.sc_disconnected_user_cancelled));
                    this.finish();
                })
                .setOnCancelListener((dialog) -> {
                    TCPClient.getInstance().disconnect(getString(R.string.sc_disconnected_user_cancelled));
                    this.finish();
                })
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AreasParsedEvent event) {
        this.addMessage(getString(R.string.sc_done));

        Toast.makeText(getApplicationContext(),
                R.string.conn_connected, Toast.LENGTH_LONG)
                .show();

        progressBar.setVisibility(View.GONE);

        Intent intent = new Intent(this, EngineRequest.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(HandShakeEvent event) {
        if ((event.getParsed().size() < 3) || (!Arrays.asList(serverSupportedVersions).contains(event.getParsed().get(2))))
            messages.add(getString(R.string.sc_version_warning));
        else
            messages.add(getString(R.string.sc_connection_ok));

        messagesAdapter.notifyDataSetChanged();

        if (TCPClient.getInstance().server.username.isEmpty() ||
                TCPClient.getInstance().server.password.isEmpty()) {
            messages.add(getString(R.string.sc_auth_wait));
            progressBar.setVisibility(View.GONE);
            editLogin(getString(R.string.login_enter));
        } else {
            messages.add(getString(R.string.sc_authorizing));
            TCPClient.getInstance().send("-;LOK;G;AUTH;{" +
                    TCPClient.getInstance().server.username + "};" +
                    TCPClient.getInstance().server.password);
            messagesAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(GlobalAuthEvent event) {
        if (event.getParsed().get(4).equalsIgnoreCase("OK")) {
            messages.add(getString(R.string.sc_auth_ok));
            messages.add(getString(R.string.sc_getting_ors));
            TCPClient.getInstance().send("-;OR-LIST");
        } else {
            messages.add(getString(R.string.sc_auth_err));
            if (event.getParsed().size() >= 6)
                messages.add(event.getParsed().get(5));
            progressBar.setVisibility(View.GONE);
            editLogin((event.getParsed().size() >= 6) ? event.getParsed().get(5) : getString(R.string.sc_auth_err));
        }
        messagesAdapter.notifyDataSetChanged();
    }

    private void start() {
        final Bundle extras = getIntent().getExtras();

        if (extras == null) {
            finish();
            return;
        }

        if (TCPClient.getInstance().connected())
            TCPClient.getInstance().disconnect("Disconnect before new connect");

        String type = extras.getString("serverType");
        int id = extras.getInt("serverId");
        Server server = (type.equals("stored")) ? ServerDb.instance.stored.get(id) : ServerDb.instance.found.get(id);

        messages.clear();
        this.addMessage(getString(R.string.sc_connecting));
        progressBar.setVisibility(View.VISIBLE);

        try {
            TCPClient.getInstance().connect(server);
        } catch (Exception e) {
            Log.e("TCP", "Connecting", e);
            addConnectError(e.getMessage());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConnectionEstablishedEvent event) {
        // connection established -> begin handshake
        TCPClient.getInstance().send("-;HELLO;" + TCPClient.PROTOCOL_VERSION_CLIENT + ";" + TCPClient.PROTOCOL_APP_NAME);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPConnectingErrorEvent event) {
        this.addConnectError(event.getError());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPDisconnectedEvent event) {
        this.addConnectError(event.getError());
    }

    private void addConnectError(String error) {
        progressBar.setVisibility(View.GONE);
        this.addMessage(getString(R.string.sc_connect_error) + ":\n" + error);
    }

    private void addMessage(String message) {
        messages.add(message);
        messagesAdapter.notifyDataSetChanged();
    }
}