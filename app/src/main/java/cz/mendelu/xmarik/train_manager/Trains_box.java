package cz.mendelu.xmarik.train_manager;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.CriticalErrorEvent;
import cz.mendelu.xmarik.train_manager.events.RefuseEvent;
import cz.mendelu.xmarik.train_manager.events.ServerOkEvent;
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
    ArrayAdapter<String> lAdapter;
    Button sendButton;
    EditText messageForServer;
    int focused;
    AlertDialog.Builder connectionDialog;
    Dialog dialog;
    TextView dialogMessage;
    Button dialogButton;
    View lastSelected;
    private ListView trains;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trains_box);
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.train_request_dialog);
        dialog.setTitle(R.string.žádost);
        dialogMessage = (TextView) dialog.findViewById(R.id.dialogMessage);
        dialogButton = (Button) dialog.findViewById(R.id.cancelButton);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                cancelMessage();
            }
        });

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
        context = this;
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        trains = (ListView) findViewById(R.id.nav_trains);
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
        } else {
            array = new ArrayList<>();
            Toast.makeText(getApplicationContext(),
                    "nebyl vybrán server", Toast.LENGTH_LONG)
                    .show();
            sendButton.setEnabled(false);
            trains.setEnabled(false);
        }
        lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array);
        trains.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                view.setBackgroundColor(Color.rgb(153, 204, 255));
                if (lastSelected != null && !lastSelected.equals(view)) {
                    lastSelected.setBackgroundColor(Color.rgb(238, 238, 238));
                }
                lastSelected = view;

                focused = position;
            }
        });

        messageForServer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (messageForServer.isFocused()) messageForServer.setText("");
            }
        });

        trains.setAdapter(lAdapter);
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
        if (this.sendButton.getText().equals("zrusit")) this.sendButton.setText(R.string.poslatZ);
        Toast.makeText(getApplicationContext(),
                R.string.novaLoko, Toast.LENGTH_LONG)
                .show();
        //TODO podminka s automatickym prechodem
        EventBus.getDefault().unregister(this);
        dialog.dismiss();
        Intent intent = new Intent(this, TrainHandler.class);
        startActivity(intent);
    }

    @Subscribe
    public void onEvent(RefuseEvent event) {
        dialog.dismiss();
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
        this.sendButton.setText(R.string.poslatZ);
    }

   /* @Subscribe
    public void onEvent(FreeEvent event) {
        Toast.makeText(getApplicationContext(),
                "loko uvolneno", Toast.LENGTH_LONG)
                .show();
        reloadEventHelper();
        this.sendButton.setText(R.string.poslatZ);
    }*/

    @Subscribe
    public void onEvent(ServerOkEvent event) {
        dialogMessage.setText("Žádost odeslána, čekám na dispečera.");
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.farServers) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            String[] menuItems = {"info", "přejít k řízení", "Uvolnit"};
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    /**
     * method for handl send button event pressed
     *
     * @param v
     */
    public void messagePressed(View v) {
        if (trains.getItemAtPosition(focused) != null) {
            final String itemValue = (String) trains.getItemAtPosition(focused);
            final Server s = ServerList.getInstance().getActiveServer();
            String serverMessage = s.getAreaServerString(itemValue);
            String msg = messageForServer.getText().toString();
            Log.e("tcp", "zadáno:" + msg + " \n");
            serverMessage = serverMessage + "{" + msg + "}\n";
            dialog.setTitle(getString(R.string.dialogAreaTitle) + msg);
            sendNext(serverMessage);
            lAdapter.notifyDataSetChanged();
            mProgressBar.setVisibility(View.VISIBLE);
            this.trains.setClickable(false);

            dialogMessage.setText(R.string.zadostOdeslana);
            dialog.show();
        }
    }

    private void cancelMessage() {
        sendNext("-;LOK;G;CANCEL;\n");
        mProgressBar.setVisibility(View.GONE);
        this.sendButton.setText(R.string.poslatZ);
        this.trains.setClickable(true);
    }


    /**
     * when there is new loko in server answer, this method will be called
     * purpose is reload all list adapter and make progress bar invisible
     */
    private void reloadEventHelper() {
        mProgressBar.setVisibility(View.GONE);
        final ArrayList<String> array;
        final ArrayList<String> acquired;
        array = active.getUnAuthorizedAreas();
        acquired = active.getAuthorizedTrainsString();
        final ArrayAdapter<String> lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array);

        final ArrayAdapter<String> ackAdapter = new ArrayAdapter<String>(this,
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
            dialog.dismiss();
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_server) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, About.class);
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

        } else if (id == R.id.nav_ack_trains) {
            Intent intent = new Intent(this, AckTrains.class);
            startActivity(intent);

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        dialog.dismiss();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        //connectionDialog.dismiss();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void criticalError(CriticalErrorEvent event) {
        ServerList.getInstance().deactivateServer();
        if (event.getMessage().startsWith("connection")) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(),
                    event.getMessage(),
                    Toast.LENGTH_LONG).show();
            //possibility of another activity, but need additional analyze
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

}
