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
        if(EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (TCPClientApplication.getInstance().connected())
            TCPClientApplication.getInstance().disconnect("Intentional disconnect");
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        mName.setText(TCPClientApplication.getInstance().server.username);
        savebox.setChecked(preferences.getBoolean("RememberPasswordDefault", true));

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

                    messages.add(getString(R.string.sc_authorizing));
                    messagesAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.VISIBLE);
                })
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AreasParsedEvent event) {
        messages.add(getString(R.string.sc_done));
        messagesAdapter.notifyDataSetChanged();

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
            messages.add(getString(R.string.sc_version_warning));
        else
            messages.add(getString(R.string.sc_connection_ok));

        messagesAdapter.notifyDataSetChanged();

        if (TCPClientApplication.getInstance().server.username.isEmpty() ||
                TCPClientApplication.getInstance().server.password.isEmpty()) {
            messages.add(getString(R.string.sc_auth_wait));
            progressBar.setVisibility(View.GONE);
            editLogin(getString(R.string.login_enter));
        } else {
            messages.add(getString(R.string.sc_authorizing));
            TCPClientApplication.getInstance().send("-;LOK;G;AUTH;{" +
                    TCPClientApplication.getInstance().server.username + "};" +
                    TCPClientApplication.getInstance().server.password);
            messagesAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(GlobalAuthEvent event) {
        if (event.getParsed().get(4).equalsIgnoreCase("OK")) {
            messages.add(getString(R.string.sc_auth_ok));
            messages.add(getString(R.string.sc_getting_ors));
            TCPClientApplication.getInstance().send("-;OR-LIST");
        } else {
            messages.add(getString(R.string.sc_auth_err));
            if (event.getParsed().size() >= 6)
                messages.add(event.getParsed().get(5));
            progressBar.setVisibility(View.GONE);
            if (event.getParsed().size() >= 6)
                editLogin(event.getParsed().get(5));
            else
                editLogin(getString(R.string.sc_auth_err));
        }
        messagesAdapter.notifyDataSetChanged();
    }

    private void start() {
        final Bundle extras = getIntent().getExtras();

        if (extras == null) {
            finish();
            return;
        }

        if (TCPClientApplication.getInstance().connected())
            TCPClientApplication.getInstance().disconnect("Disconnect before new connect");

        String type = extras.getString("serverType");
        int id = extras.getInt("serverId");
        Server server = (type.equals("stored")) ? ServerDb.instance.stored.get(id) : ServerDb.instance.found.get(id);

        messages.clear();
        messages.add(getString(R.string.sc_connecting));
        progressBar.setVisibility(View.VISIBLE);
        messagesAdapter.notifyDataSetChanged();

        try {
            TCPClientApplication.getInstance().connect(server);
        } catch (Exception e) {
            Log.e("TCP", "Connecting", e);
            addConnectError(e.getMessage());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConnectionEstablishedEvent event) {
        // connection established -> begin handshake
        TCPClientApplication.getInstance().send("-;HELLO;1.1");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPConnectingErrorEvent event) {
        this.addConnectError(event.getError());
    }

    public void addConnectError(String error) {
        progressBar.setVisibility(View.GONE);
        messages.add(getString(R.string.sc_connect_error) + ":\n" + error);
        messagesAdapter.notifyDataSetChanged();
    }
}