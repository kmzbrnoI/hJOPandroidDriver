package cz.mendelu.xmarik.train_manager.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.HelpServices;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.Server;
import cz.mendelu.xmarik.train_manager.ServerList;
import cz.mendelu.xmarik.train_manager.events.CriticalErrorEvent;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class Servers extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String MyPREFERENCES = "Servers";
    private final int port = 5880;
    Button lButton;
    ArrayList<String> array;
    ArrayList<String> array1;
    ArrayAdapter<String> fAdapter;
    ArrayAdapter<String> lAdapter;
    Context context;
    SharedPreferences sharedpreferences;
    private ListView lServers;
    private ListView fServers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servers);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        context = this.getApplicationContext();
        lButton = (Button) findViewById(R.id.serverButton);
        fServers = (ListView) findViewById(R.id.farServers);
        lServers = (ListView) findViewById(R.id.localServers);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        array = ServerList.getInstance().getServersString();
        lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array);
        array1 = ServerList.getInstance().getStoredServersString();
        fAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array1);
        lServers.setAdapter(lAdapter);
        fServers.setAdapter(fAdapter);
        registerForContextMenu(fServers);
        // ListView Item Click Listener
        fServers.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // ListView Clicked item index
                int itemPosition = position;
                // ListView Clicked item value
                String itemValue = (String) fServers.getItemAtPosition(position);
                AuthorizeServer(itemValue);
            }

        });

        lButton.setOnClickListener(
                new View.OnClickListener() {

                    public void onClick(View view) {
                        //tady to funguje
                        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                        if (mWifi.isConnected()) {
                            //UdpDiscover udp = new UdpDiscover(context,port, );
                            System.out.print("budon activated");
                            //udp.run();
                            //   udp.execute();
                            Toast.makeText(getApplicationContext(),
                                    "server discover method done", Toast.LENGTH_LONG)
                                    .show();
                        } else Toast.makeText(getApplicationContext(),
                                "Wifi připojení není dostupné", Toast.LENGTH_LONG)
                                .show();
                    }
                });

        // ListView Item Click Listener
        lServers.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // ListView Clicked item value
                String itemValue = (String) lServers.getItemAtPosition(position);
                AuthorizeServer(itemValue);

            }


        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.farServers) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(array1.get(info.position));
            String[] menuItems = {"zvolit", "přihlašovací údaje", "změnit", "info", "smazat", "smazat vše"};
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String listItemName = array1.get(info.position);
        String value = listItemName;
        String[] tmp = value.split("\t");
        //TODO dialogy k mazani
        switch (menuItemIndex) {
            case 0:
                Server tmpServer = ServerList.getInstance().getServer(tmp[1]);
                if (tmpServer.status == true) {
                    AuthorizeServer(tmp[0]);
                } else Toast.makeText(getApplicationContext(),
                        "server je offline", Toast.LENGTH_LONG)
                        .show();
                break;
            case 1:
                showDialog(tmp[0]);
                break;
            case 2:
                Intent intent = new Intent(getBaseContext(), NewServer.class);
                intent.putExtra("server", listItemName);
                startActivityForResult(intent, 2);
                break;
            case 3:
                Toast.makeText(getApplicationContext(),
                        ServerList.getInstance().getServer(tmp[0]).getInfo(), Toast.LENGTH_LONG)
                        .show();
                break;
            case 4:
                array1.remove(info.position);
                ServerList.getInstance().removeServer(info.position);
                fAdapter.notifyDataSetChanged();
                break;
            case 5:
                ServerList.getInstance().clearCustomServer();
                deleteAllServers();
                break;

        }
        return true;
    }

    public void discoverServer(View view) {

        Toast.makeText(getApplicationContext(),
                "discover clicked", Toast.LENGTH_LONG)
                .show();
    }

    public void addServer(View view) {
        Intent intent = new Intent(this, NewServer.class);
        startActivityForResult(intent, 1);
    }

    public void AuthorizeServer(String itemValue) {

        Intent intent = new Intent(getBaseContext(), ServerConnector.class);
        intent.putExtra("server", itemValue);
        startActivityForResult(intent, 2);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            array1 = ServerList.getInstance().getStoredServersString();
            fAdapter.notifyDataSetChanged();
            //asi predelat s tim novym adapterem do ifu, pak to vali
            fAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, array1);
            fAdapter.notifyDataSetChanged();
            fServers.setAdapter(fAdapter);
            fAdapter.notifyDataSetChanged();
            String txt = ServerList.getInstance().getServerStoreString();
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString("storedServers", txt);
            editor.commit();
            if (resultCode == RESULT_OK) {
                array1 = ServerList.getInstance().getStoredServersString();
                fAdapter.notifyDataSetChanged();
            }
            if (resultCode == RESULT_CANCELED) {
                //Do nothing?
            }
        } else if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(),
                        "Server authorized and connected", Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "Server authorization failed", Toast.LENGTH_LONG)
                        .show();
            }
        }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_server) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent);

        } else if (id == R.id.nav_train_manage) {
            Intent intent = new Intent(this, TrainHandler.class);
            startActivity(intent);

        } else if (id == R.id.nav_trains) {
            Intent intent = new Intent(this, TrainRequest.class);
            startActivity(intent);

        } else if (id == R.id.nav_view) {
            Intent intent = new Intent(this, Servers.class);
            startActivity(intent);

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void showDialog(final String serverName) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.user_dialog);
        dialog.setTitle("Set user data");
        final Server server = ServerList.getInstance().getServer(serverName);
        //set dialog component
        final EditText mName = (EditText) dialog.findViewById(R.id.dialogName);
        final EditText mPasswd = (EditText) dialog.findViewById(R.id.dialogPasswd);
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
        // if button is clicked, close the custom dialog
        if (server.getUserPassword() != null) mPasswd.setText(server.getUserPassword());
        if (server.getUserName() != null) mName.setText(server.getUserName());

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                server.setUserPassword(HelpServices.hashPasswd(mPasswd.getText().toString()));
                server.setUserName(mName.getText().toString());
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void deleteAllServers() {
        sharedpreferences = getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.remove("StoredServers");
        editor.commit();
        Toast.makeText(getApplicationContext(),
                "servery byly smazány",
                Toast.LENGTH_LONG).show();
    }

    @Subscribe
    public void criticalError(CriticalErrorEvent event) {
        ServerList.getInstance().deactivateServer();
        if (event.getMessage().startsWith("connection")) {
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);
        }else {
            Toast.makeText(getApplicationContext(),
                    event.getMessage(),
                    Toast.LENGTH_LONG).show();
            //possibility of another activity, but need additional analyze
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);
        }
    }
}
