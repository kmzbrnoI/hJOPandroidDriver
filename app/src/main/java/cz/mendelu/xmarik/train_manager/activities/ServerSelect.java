package cz.mendelu.xmarik.train_manager.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.FoundServersReloadEvent;
import cz.mendelu.xmarik.train_manager.network.UDPDiscover;
import cz.mendelu.xmarik.train_manager.storage.ControlAreaDb;
import cz.mendelu.xmarik.train_manager.helpers.HashHelper;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;
import cz.mendelu.xmarik.train_manager.storage.TrainDb;
import cz.mendelu.xmarik.train_manager.events.StoredServersReloadEvent;
import cz.mendelu.xmarik.train_manager.models.Server;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class ServerSelect extends NavigationBase {
    Button lButton;
    ArrayAdapter<String> fAdapter;
    ArrayAdapter<String> sAdapter;
    ArrayList<String> found;
    ArrayList<String> stored;

    SharedPreferences sharedpreferences;
    ListView sServers;
    ListView fServers;

    public enum Source {
        FOUND,
        STORED
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_server_select);
        super.onCreate(savedInstanceState);

        Object obj = this;
        Context context = this.getApplicationContext();

        // create database of servers
        ServerDb.instance = new ServerDb();

        // create database of control areas
        ControlAreaDb.instance = new ControlAreaDb();

        // create database of trains
        TrainDb.instance = new TrainDb();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lButton = (Button) findViewById(R.id.serverButton);
        fServers = (ListView) findViewById(R.id.foundServers);
        sServers = (ListView) findViewById(R.id.storedServers);

        // load shared preferences
        sharedpreferences = getDefaultSharedPreferences(getApplicationContext());
        if (sharedpreferences.contains("StoredServers"))
            ServerDb.instance.loadServers(sharedpreferences.getString("StoredServers", ""));

        // run UDP discover:
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected()) {
            (new UDPDiscover((WifiManager)context.getSystemService(Context.WIFI_SERVICE))).execute();
        } else Toast.makeText(getApplicationContext(),
                R.string.conn_wifi_unavailable, Toast.LENGTH_LONG)
                .show();

        // bind ListView adapters:
        found = new ArrayList<String>();
        fAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, found);
        fServers.setAdapter(fAdapter);

        stored = new ArrayList<String>();
        sAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, stored);
        sServers.setAdapter(sAdapter);

        registerForContextMenu(sServers);

        fServers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                connect(Source.FOUND, position);
            }
        });

        sServers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                connect(Source.STORED, position);
            }
        });
    }

    public void updateStoredServers() {
        stored.clear();
        for (Server s : ServerDb.instance.stored)
            stored.add(s.name + "\t" + s.host + "\n" + s.type);

        sAdapter.notifyDataSetChanged();
    }

    public void updateFoundServers() {
        found.clear();
        for (Server s : ServerDb.instance.found) {
            String statusText = s.active ? "online" : "offline";
            found.add(s.name + "\t" + s.host + "\n" + s.type + " \t" + statusText);
        }

        fAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.storedServers) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(ServerDb.instance.stored.get(info.position).host);

            String[] menuItems = {
                    getString(R.string.mm_connect),
                    getString(R.string.mm_change_login),
                    getString(R.string.mm_change_settings),
                    getString(R.string.mm_delete),
                    getString(R.string.mm_delete_all)
            };

            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        Server s = ServerDb.instance.stored.get(info.position);

        switch (menuItemIndex) {
            case 0:
                if (s.active) {
                    connect(Source.STORED, info.position);
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.conn_server_offline)
                            .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    connect(Source.FOUND, info.position);
                                }
                            })
                            .setNegativeButton("no", new DialogInterface.OnClickListener() { // TODO: strings here
                                @Override
                                public void onClick(DialogInterface dialog, int which) {}
                            }).show();
                }
                break;

            case 1:
                changeLogin(s);
                break;

            case 2:
                Intent intent = new Intent(getBaseContext(), ServerEdit.class);
                intent.putExtra("serverId", info.position);
                startActivity(intent);
                break;

            case 3:
                ServerDb.instance.removeStoredServer(info.position);
                break;

            case 4:
                ServerDb.instance.clearStoredServers();
                break;
        }
        return true;
    }

    public void addServerClick(View view) {
        startActivity(new Intent(getBaseContext(), ServerEdit.class));
    }

    public void discoverServerClick(View view) {
        Context context = this.getApplicationContext();

        ServerDb.instance.clearFoundServers();
        fAdapter.notifyDataSetChanged();

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected()) {
            (new UDPDiscover((WifiManager)context.getSystemService(Context.WIFI_SERVICE))).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            float deg = lButton.getRotation() + 720F;
            lButton.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.conn_wifi_unavailable)
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
        }

        //float deg = lButton.getRotation() + 360F;
        //lButton.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void connect(Source from, int index) {
        Intent intent = new Intent(getBaseContext(), ServerConnector.class);
        intent.putExtra("serverType", from == Source.FOUND ? "found" : "stored");
        intent.putExtra("serverId", index);
        startActivityForResult(intent, 2);
    }

    /**
     * This method is called when activity for result ended.
     * metoda slouží k obsloužení výsledku
     * @param requestCode číslo pro identifikaci volní (určí o kterou aktivitu se jedná)
     * @param resultCode hodnota vyjadřující zda aktivita zkončila úspěchem či nikoli
     * @param data případné návratové hodnoty
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*if (requestCode == 1) {
            array1 = ServerDb.getInstance().getStoredServersString();
            fAdapter.notifyDataSetChanged();
            //asi predelat s tim novym adapterem do ifu, pak to vali
            fAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, array1);
            fAdapter.notifyDataSetChanged();
            fServers.setAdapter(fAdapter);
            fAdapter.notifyDataSetChanged();
            String txt = ServerDb.getInstance().getServerStoreString();
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString("storedServers", txt);
            //editor.commit();
            editor.apply();
            if (resultCode == RESULT_OK) {
                array1 = ServerDb.getInstance().getStoredServersString();
                fAdapter.notifyDataSetChanged();
            }else if (resultCode == RESULT_CANCELED) {
                //Do nothing?
            }
        } else if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(),
                        R.string.conn_connected, Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(getApplicationContext(),
                        R.string.conn_no_server_authorized, Toast.LENGTH_LONG)
                        .show();
            }
        }*/
    }//onActivityResult

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void changeLogin(final Server server) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_user);
        dialog.setTitle("Set user data");

        //set dialog component
        final EditText mName = (EditText) dialog.findViewById(R.id.dialogName);
        final EditText mPasswd = (EditText) dialog.findViewById(R.id.dialogPasswd);
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
        // if button is clicked, close the custom dialog
        if (server.password != null) mPasswd.setText(server.password);
        if (server.username != null) mName.setText(server.username);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                server.password = HashHelper.hashPasswd(mPasswd.getText().toString());
                server.username = mName.getText().toString();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Subscribe
    public void onEvent(StoredServersReloadEvent event) {
        updateStoredServers();
    }

    @Subscribe
    public void onEvent(FoundServersReloadEvent event) {
        updateFoundServers();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        save();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFoundServers();
        updateStoredServers();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    public void save() {
        String n = ServerDb.instance.getSaveString();

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.remove("StoredServers");
        editor.clear();
        editor.putString("StoredServers", n);
        editor.commit();
    }

    /*@Subscribe
    public void tcpErrorEvent(TCPDisconnectEvent event) {
        ServerDb.getInstance().deactivateServer();
        Toast.makeText(getApplicationContext(),
                event.getError(),
                Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, ServerSelect.class);
        startActivity(intent);
    }*/

}