package cz.mendelu.xmarik.train_manager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class Servers extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final int port = 5880;
    private ListView lServers;
    private ListView  fServers;
    Button lButton;
    ArrayList<String> array;
    ArrayList <String> array1;
    ArrayAdapter<String> fAdapter;
    ArrayAdapter<String> lAdapter;
    Context context;
    SharedPreferences sharedpreferences;
    public static final String MyPREFERENCES = "Servers" ;

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
        lButton = (Button)findViewById(R.id.serverButton);
        fServers = (ListView) findViewById(R.id.farServers);
        lServers = (ListView) findViewById(R.id.localServers);

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        array = ServerList.getInstance().getServersString();

        lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array );

        array1 = ServerList.getInstance().getStoredServersString();
        fAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array1 );

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
                        }else  Toast.makeText(getApplicationContext(),
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
        if (v.getId()==R.id.farServers) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            menu.setHeaderTitle(array1.get(info.position));
            String[] menuItems = {"zvolit","přihlašovací údaje","změnit","smazat"};
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] menuItems = {"zvolit","přihlašovací údaje","změnit","smazat"};
        String listItemName = array1.get(info.position);

        switch (menuItemIndex)
        {
            case 0:
                AuthorizeServer(listItemName);
                break;
            case 1:

                break;
            case 2:
                break;
            case 3:
                array1.remove(info.position);
                ServerList.getInstance().removeServer(info.position);
                fAdapter.notifyDataSetChanged();
            /*String[] tmpAray = new String[array1.size()-1];
            int p = 0;
            for(int i=0; i < array1.size(); i++)
            {
                if(i!=info.position){
                    tmpAray[p] = array1.get(info.position);
                    p++;
                }
            }*/
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
        startActivityForResult(intent,2);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
            Intent intent = new Intent(this, Servers.class);
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
            Intent intent = new Intent(this, Trains_box.class);
            startActivity(intent);

        } else if (id == R.id.nav_view) {
            Intent intent = new Intent(this, Servers.class);
            startActivity(intent);

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
