package cz.mendelu.xmarik.train_manager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;

public class NewServer extends AppCompatActivity {

    Server server = null;
    private EditText nameText, ipAdrText, portText, aboutText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_new);
        nameText = (EditText) findViewById(R.id.nameText);
        ipAdrText = (EditText) findViewById(R.id.ipText);
        portText = (EditText) findViewById(R.id.portText);
        aboutText = (EditText) findViewById(R.id.aboutText);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String value = extras.getString("server");
            String[] tmp = value.split("\t");
            server = ServerDb.getInstance().getServer(tmp[0]);
            nameText.setText(server.name);
            ipAdrText.setText(server.host);
            portText.setText(String.valueOf(server.port));
            aboutText.setText(server.type);
        }
    }

    public void save(View view) {
        String name, port, ipAdr, about;
        name = nameText.getText().toString();
        port = portText.getText().toString();
        ipAdr = ipAdrText.getText().toString();
        about = aboutText.getText().toString();

        if (name.equals("") || port.equals("") || ipAdr.equals("")) {
            Toast.makeText(getApplicationContext(),
                    R.string.ns_warning_compulsory, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (ServerDb.getInstance().getStoredServersString().contains(name)) {
            Toast.makeText(getApplicationContext(),
                    R.string.ns_warning_server_exists, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (name.contains("--")) {
            Toast.makeText(getApplicationContext(),
                    R.string.ns_warning_invalid_characters, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (server == null) {
            server = new Server(name, ipAdr, Integer.parseInt(port), false, about, "", "");
        } else {
            server.name = name;
            server.port = Integer.parseInt(port);
            server.type = about;
            server.active = false;
        }

        ServerDb.getInstance().addCustomServer(server);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", name);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public void back(View view) {
        onBackPressed();
    }
}
