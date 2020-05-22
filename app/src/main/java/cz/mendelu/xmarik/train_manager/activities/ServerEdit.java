package cz.mendelu.xmarik.train_manager.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.greenrobot.eventbus.EventBus;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.StoredServersReloadEvent;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;

public class ServerEdit extends AppCompatActivity {
    Server server = null;
    private EditText nameText, ipAdrText, portText, aboutText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_edit);
        nameText = findViewById(R.id.nameText);
        ipAdrText = findViewById(R.id.ipText);
        portText = findViewById(R.id.portText);
        aboutText = findViewById(R.id.aboutText);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            server = ServerDb.instance.stored.get(extras.getInt("serverId"));

            nameText.setText(server.name);
            ipAdrText.setText(server.host);
            portText.setText(String.valueOf(server.port));
            aboutText.setText(server.type);
        } else {
            server = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.navigation_server_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                save(item.getActionView());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void save(View view) {
        String name, port, ipAdr, about;
        name = nameText.getText().toString();
        port = portText.getText().toString();
        ipAdr = ipAdrText.getText().toString();
        about = aboutText.getText().toString();

        if (name.equals("") || port.equals("") || ipAdr.equals("")) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.ns_warning_compulsory)
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
            return;
        }

        if (name.contains("--")) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.ns_warning_invalid_characters)
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
            return;
        }

        if (server == null) {
            ServerDb.instance.addStoredServer(new Server(name, ipAdr, Integer.parseInt(port), false, about, "", ""));
            ServerDb.instance.saveServers();
        } else {
            server.name = name;
            server.port = Integer.parseInt(port);
            server.type = about;
            server.active = false;
            ServerDb.instance.saveServers();
            EventBus.getDefault().post(new StoredServersReloadEvent());
        }

        finish();
    }

    public void back(View view) {
        onBackPressed();
    }
}
