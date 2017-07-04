package cz.mendelu.xmarik.train_manager.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
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

        final Context obj = this;
        Context context = this.getApplicationContext();

        // create database of servers
        ServerDb.instance = new ServerDb(getDefaultSharedPreferences(getApplicationContext()));

        // create database of control areas
        ControlAreaDb.instance = new ControlAreaDb();

        // create database of trains
        TrainDb.instance = new TrainDb();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lButton = (Button) findViewById(R.id.serverButton);
        fServers = (ListView) findViewById(R.id.foundServers);
        sServers = (ListView) findViewById(R.id.storedServers);

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
                                    final int position, long id) {
                if (ServerDb.instance.found.get(position).active) {
                    connect(Source.FOUND, position);
                } else {
                    new AlertDialog.Builder(obj)
                            .setMessage(R.string.conn_server_offline)
                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    connect(Source.FOUND, position);
                                }
                            })
                            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {}
                            }).show();
                }
            }
        });

        sServers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {
                if (ServerDb.instance.stored.get(position).active) {
                    connect(Source.STORED, position);
                } else {
                    new AlertDialog.Builder(obj)
                            .setMessage(R.string.conn_server_offline)
                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    connect(Source.STORED, position);
                                }
                            })
                            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                }
            }
        });

        // run UDP discover:
        if (isWifiOnAndConnected()) {
            (new UDPDiscover((WifiManager)context.getSystemService(Context.WIFI_SERVICE))).execute();
            float deg = lButton.getRotation() + 720F;
            lButton.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
        } else Toast.makeText(getApplicationContext(),
                R.string.conn_wifi_unavailable, Toast.LENGTH_LONG)
                .show();
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
                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    connect(Source.FOUND, info.position);
                                }
                            })
                            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
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
                new AlertDialog.Builder(this)
                        .setMessage(R.string.conn_delete_server)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ServerDb.instance.removeStoredServer(info.position);
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        }).show();
                break;

            case 4:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.conn_delete_all)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ServerDb.instance.clearStoredServers();
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        }).show();
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

        if (isWifiOnAndConnected()) {
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
    }

    private boolean isWifiOnAndConnected() {
        Context context = this.getApplicationContext();
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            return !( wifiInfo.getNetworkId() == -1 );
        }
        else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    public void connect(Source from, int index) {
        Intent intent = new Intent(getBaseContext(), ServerConnector.class);
        intent.putExtra("serverType", from == Source.FOUND ? "found" : "stored");
        intent.putExtra("serverId", index);
        startActivity(intent);
    }

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

        //set dialog component
        final EditText mName = (EditText) dialog.findViewById(R.id.dialogName);
        final EditText mPasswd = (EditText) dialog.findViewById(R.id.dialogPasswd);
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
        final TextView mMessage = (TextView) dialog.findViewById(R.id.tv_note);
        final CheckBox savebox = (CheckBox) dialog.findViewById(R.id.dialogSaveData);

        mMessage.setText("Edit login");
        mName.setText(server.username);
        mPasswd.setText("");
        savebox.setChecked(true);
        savebox.setEnabled(false);

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
        if(EventBus.getDefault().isRegistered(this))EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFoundServers();
        updateStoredServers();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

}