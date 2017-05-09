package cz.mendelu.xmarik.train_manager.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.Server;
import cz.mendelu.xmarik.train_manager.ServerList;
import cz.mendelu.xmarik.train_manager.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.Train;
import cz.mendelu.xmarik.train_manager.events.FreeEvent;
import cz.mendelu.xmarik.train_manager.events.RefuseEvent;
import cz.mendelu.xmarik.train_manager.events.TrainReloadEvent;

public class AckTrains extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Server active;
    int port;
    String ipAdr;
    String serverResponse = "-;LOK;G:PLEASE-RESP;";
    Context context;
    ArrayList<String> array;
    ArrayAdapter<String> lAdapter;
    Button sendButton;
    Integer focused;
    private ListView trains;
    private ListView ackTrains;
    private List<String> lokos;
    AlertDialog.Builder connectionDialog;
    View lastSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ack_trains);

        connectionDialog = new AlertDialog.Builder(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        lokos = new LinkedList<>();
        context = this;
        trains = (ListView) findViewById(R.id.acquiredTrains);
        sendButton = (Button) findViewById(R.id.trainBoxButton);
        focused = -1;
        active = ServerList.getInstance().getActiveServer();
        EventBus.getDefault().register(this);
        if (active != null) {
            port = active.port;
            ipAdr = active.ipAdr;
            array = active.getAuthorizedTrainsString();
        } else {
            array = new ArrayList<>();
            Toast.makeText(getApplicationContext(),
                    "nebyl vybrán server", Toast.LENGTH_LONG)
                    .show();
            sendButton.setEnabled(false);
            trains.setEnabled(false);
            ackTrains.setEnabled(false);
        }
        lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array);
        trains.setAdapter(lAdapter);
        trains.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // ListView Clicked item index
                view.setBackgroundColor(Color.rgb(153,204,255));
                if (lastSelected != null && !lastSelected.equals(view)) {
                    lastSelected.setBackgroundColor(Color.rgb(238,238,238));
                }
                lastSelected = view;
                focused = position;
            }

        });
    }

    private void sendNext(String message) {
        //sends the message to the server
        if (TCPClientApplication.getInstance().getClient() != null) {
            TCPClientApplication.getInstance().getClient().sendMessage(message);
            Log.e("tcp", "odeslano:" + message + " \n");
        }
    }

    @Subscribe
    public void onEvent(TrainReloadEvent event) {
        // your implementation
        reloadEventHelper();
        if (this.sendButton.getText().equals("zrusit")) this.sendButton.setText("@string/uvolnit");
        Toast.makeText(getApplicationContext(),
                "Zářízení autorizováno", Toast.LENGTH_LONG)
                .show();
        //TODO podminka s automatickym prechodem
        EventBus.getDefault().unregister(this);
        Intent intent = new Intent(this, TrainHandler.class);
        startActivity(intent);
    }

    @Subscribe
    public void onEvent(RefuseEvent event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(event.getMessage())
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
        this.sendButton.setText(R.string.uvolnit);
    }

    @Subscribe
    public void onEvent(FreeEvent event) {
        Toast.makeText(getApplicationContext(),
                R.string.lokoUvolneno, Toast.LENGTH_LONG)
                .show();
        reloadEventHelper();
        this.sendButton.setText(R.string.uvolnit);
    }

    public void release(View v) {
        if ( focused != null && trains.getItemAtPosition(focused) != null ) {
            final String itemValue = (String) trains.getItemAtPosition(focused);
            final Server s = ServerList.getInstance().getActiveServer();
            Train train = s.getTrain(itemValue.substring(0,itemValue.indexOf("\n")));
            String message = "-;LOK;"+train.getName()+";RELEASE";
            sendNext(message);
            ServerList.getInstance()
                    .getActiveServer().removeTrain(train);

            reloadEventHelper();
        }
    }

    private void reloadEventHelper() {
        final ArrayList<String> acquired;
        this.focused = null;
        acquired = active.getAuthorizedTrainsString();

        lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, acquired);
        trains.setAdapter(lAdapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            EventBus.getDefault().unregister(this);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_server) {
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent);

        } else if (id == R.id.nav_train_manage) {
            Intent intent = new Intent(this, TrainHandler.class);
            startActivity(intent);

        } else if (id == R.id.nav_ack_trains) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

        } else if (id == R.id.nav_view) {
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);

        } else if (id == R.id.nav_trains) {
            Intent intent = new Intent(this, TrainRequest.class);
            startActivity(intent);

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!EventBus.getDefault().isRegistered(this))EventBus.getDefault().register(this);
        if(lAdapter != null) {
            array = active.getAuthorizedTrainsString();
            lAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, array);
            trains.setAdapter(lAdapter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
    }
}
