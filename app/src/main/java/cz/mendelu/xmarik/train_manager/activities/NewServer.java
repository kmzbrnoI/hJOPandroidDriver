package cz.mendelu.xmarik.train_manager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.Server;
import cz.mendelu.xmarik.train_manager.ServerList;

public class NewServer extends AppCompatActivity {

    Server server = null;
    private EditText nameText, ipAdrText, portText, aboutText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_server);
        nameText = (EditText) findViewById(R.id.nameText);
        ipAdrText = (EditText) findViewById(R.id.ipText);
        portText = (EditText) findViewById(R.id.portText);
        aboutText = (EditText) findViewById(R.id.aboutText);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String value = extras.getString("server");
            String[] tmp = value.split("\t");
            server = ServerList.getInstance().getServer(tmp[0]);
            nameText.setText(server.name);
            ipAdrText.setText(server.ipAdr);
            portText.setText(String.valueOf(server.port));
            aboutText.setText(server.about);
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
                    "Název, adresa serveru a port serveru jsou povinná pole", Toast.LENGTH_LONG)
                    .show();
        } else if (ip(ipAdr) || domainName(ipAdr)) {
            if (ServerList.getInstance().getStoredServersString().contains(name)) {
                Toast.makeText(getApplicationContext(),
                        "Server s tímto názvem již přidán :", Toast.LENGTH_LONG)
                        .show();
            } else if (name.contains("--")) {
                Toast.makeText(getApplicationContext(),
                        "Název nesmí obsahovat sekvenci \"--\"", Toast.LENGTH_LONG)
                        .show();
            } else {
                if (server == null) {
                    server = new Server(name, Integer.parseInt(port), false, about);
                } else {
                    server.name = name;
                    server.port = Integer.parseInt(port);
                    server.about = about;
                    server.setActive(false);
                }
                if (ip(ipAdr)) {
                    server.ipAdr = ipAdr;
                } else {
                    server.setDnsName(ipAdr);
                }
                ServerList.getInstance().addCustomServer(server);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", name);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        } else Toast.makeText(getApplicationContext(),
                "adresa není v platném ipV4 formátu a ani se nejedná o doménové jméno", Toast.LENGTH_LONG)
                .show();
    }

    public void back(View view) {
        onBackPressed();
    }

    public boolean ip(String text) {
        Pattern p = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
        Matcher m = p.matcher(text);
        return m.find();
    }

    public boolean domainName(String text) {
        Pattern p = Pattern.compile("^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\\\.)+[A-Za-z]{2,6}$");
        Matcher m = p.matcher(text);
        return m.find();
    }

}
