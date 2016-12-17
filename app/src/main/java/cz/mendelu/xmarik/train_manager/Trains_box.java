package cz.mendelu.xmarik.train_manager;

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
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cz.mendelu.xmarik.train_manager.events.FreeEvent;
import cz.mendelu.xmarik.train_manager.events.RefuseEvent;
import cz.mendelu.xmarik.train_manager.events.TrainReloadEvent;

public class Trains_box extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Server active;
    int port;
    String ipAdr;
    String serverResponse = "-;LOK;G:PLEASE-RESP;";
    Context context;
    ProgressBar mProgressBar;
    ArrayList<String> array;
    ArrayList<String> acquired;
    ArrayAdapter<String> ackAdapter;
    ArrayAdapter<String> lAdapter;
    Button sendButton;
    EditText messageForServer;
    int focused;
    int trainNum;
    private ListView trains;
    private ListView ackTrains;
    private List<String> lokos;
    AlertDialog.Builder connectionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trains_box);

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
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        trains = (ListView) findViewById(R.id.nav_trains);
        ackTrains = (ListView) findViewById(R.id.acquiredTrains);
        sendButton = (Button) findViewById(R.id.trainBoxButton);
        messageForServer = (EditText) findViewById(R.id.authMessage);
        focused = -1;
        active = ServerList.getInstance().getActiveServer();
        EventBus.getDefault().register(this);
        mProgressBar.setVisibility(View.GONE);
        if (active != null) {
            port = active.port;
            ipAdr = active.ipAdr;
            array = active.getUnAuthorizedAreas();
            acquired = active.getAuthorizedTrainsString();
            trainNum = acquired.size();
        } else {
            array = new ArrayList<>();
            acquired = new ArrayList<>();
            Toast.makeText(getApplicationContext(),
                    "nebyl vybrán server", Toast.LENGTH_LONG)
                    .show();
            sendButton.setEnabled(false);
            trains.setEnabled(false);
            ackTrains.setEnabled(false);
        }
        lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array);
        ackAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, acquired);
        trains.setAdapter(lAdapter);
        ackTrains.setAdapter(ackAdapter);
        trains.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // ListView Clicked item index
                int itemPosition = position;
                final String[] srt = new String[1];
                for (int i = 0; i < array.size(); i++) {
                    if (i != position) {
                        trains.getChildAt(i).setBackgroundColor(Color.WHITE);
                    } else trains.getChildAt(i).setBackgroundColor(Color.CYAN);
                }
                focused = position;
            }

        });

        messageForServer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (messageForServer.isFocused()) messageForServer.setText("");
            }
        });

        ackTrains.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // ListView Clicked item index
                int itemPosition = position;
                // ListView Clicked item value
                String itemValue = (String) ackTrains.getItemAtPosition(position);
                // Show Alert
                Server s = ServerList.getInstance().getActiveServer();
                String lokoString = itemValue.substring(0, itemValue.indexOf("\n"));
                Train t = s.getTrain(lokoString);
                sendNext("-;LOK;" + t.getName() + ";RELEASE");
                t.setAuthorized(false);

                acquired.remove(position);
                ackAdapter.notifyDataSetChanged();
                lAdapter.notifyDataSetChanged();
            }

        });
    }

    private void sendNext(String message) {
        //sends the message to the server
        if (TCPClientApplication.getInstance().getClient() != null) {
            TCPClientApplication.getInstance().getClient().sendMessage(message);
            Log.e("tcp", "odeslano:" + message + " \n");
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe
    public void onEvent(TrainReloadEvent event) {
        // your implementation
        reloadEventHelper();
        if (this.sendButton.getText().equals("zrusit")) this.sendButton.setText("poslat");
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

        mProgressBar.setVisibility(View.GONE);
        this.sendButton.setText("poslat");
    }

    @Subscribe
    public void onEvent(FreeEvent event) {
        Toast.makeText(getApplicationContext(),
                "loko uvolneno", Toast.LENGTH_LONG)
                .show();
        reloadEventHelper();
        this.sendButton.setText("poslat");
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.farServers) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(acquired.get(info.position));
            String[] menuItems = {"info", "přejít k řízení", "uvolnit"};
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String listItemName = acquired.get(info.position);
        String value = listItemName;
        Train t = ServerList.getInstance().getActiveServer().getTrain(value);
        switch (menuItemIndex) {
            case 0:
                String toastMessage = t.getUserTrainInfo();
                Toast.makeText(getApplicationContext(),
                        toastMessage, Toast.LENGTH_LONG)
                        .show();
                break;
            case 1:

                Intent intent = new Intent(this, TrainHandler.class);
                startActivity(intent);
                break;
            case 2:
                acquired.remove(info.position);
                sendNext("-;LOK;" + t.getName() + ";RELEASE\n");
                t.setAuthorized(false);
                ackAdapter.notifyDataSetChanged();
                lAdapter.notifyDataSetChanged();
                break;
        }
        return true;
    }

    public void messagePressed(View v) {
        if (this.sendButton.getText().equals("poslat")) {
            final String itemValue = (String) trains.getItemAtPosition(focused);
            final Server s = ServerList.getInstance().getActiveServer();
            final String[] serverMessage = {s.getAreaServerString(itemValue)};
            String msg = messageForServer.getText().toString();
            Log.e("tcp", "zadáno:" + msg + " \n");
            serverMessage[0] = serverMessage[0] + "{" + msg + "}\n";
            sendNext(serverMessage[0]);
            lAdapter.notifyDataSetChanged();
            ackAdapter.notifyDataSetChanged();
            mProgressBar.setVisibility(View.VISIBLE);
            this.sendButton.setText("zrusit");
            this.trains.setClickable(false);

            connectionDialog.setMessage("Probíhá žádost o loko");
            connectionDialog.setNegativeButton("Zrušit", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.dismiss();
                }
            });
            connectionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    cancelMessage();
                }
            });
            connectionDialog.show();

        } else {
            cancelMessage();
        }
    }

    private void cancelMessage() {
        sendNext("-;LOK;G;CANCEL;\n");
        mProgressBar.setVisibility(View.GONE);
        this.sendButton.setText("poslat");
        this.trains.setClickable(true);
    }


    private void reloadEventHelper() {
        mProgressBar.setVisibility(View.GONE);
        final ArrayList<String> array;
        final ArrayList<String> acquired;
        array = active.getUnAuthorizedAreas();
        acquired = active.getAuthorizedTrainsString();
        final ArrayAdapter<String> lAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array);

        final ArrayAdapter<String> ackAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, acquired);

        trains.setAdapter(lAdapter);
        ackTrains.setAdapter(ackAdapter);
        registerForContextMenu(ackTrains);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_server) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent);

        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);

        } else if (id == R.id.nav_train_manage) {
            Intent intent = new Intent(this, TrainHandler.class);
            startActivity(intent);

        } else if (id == R.id.nav_trains) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

        } else if (id == R.id.nav_view) {
            Intent intent = new Intent(this, MainActivity.class);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
    }
}
