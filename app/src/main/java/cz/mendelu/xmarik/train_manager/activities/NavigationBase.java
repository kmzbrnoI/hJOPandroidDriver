package cz.mendelu.xmarik.train_manager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;

import cz.mendelu.xmarik.train_manager.BuildConfig;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.events.LokAddEvent;
import cz.mendelu.xmarik.train_manager.events.LokChangeEvent;
import cz.mendelu.xmarik.train_manager.events.LokRemoveEvent;
import cz.mendelu.xmarik.train_manager.events.LokTotalChangeErrorEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.models.Train;
import cz.mendelu.xmarik.train_manager.network.TCPClient;
import cz.mendelu.xmarik.train_manager.storage.TrainDb;


/**
 * Class NavigationBase implements base class for all activities, which want to have navigation
 * toolbar.
 * Every activity inheriting from NavigationBase must call setContentView before .super in
 * constructor.
 * Created by Jan Horacek
 */

public class NavigationBase extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawer;
    Menu menu;
    MenuItem miTrainRequest;
    TextView tvUser;
    TextView tvServer;
    boolean isDrawerFixed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.isDrawerFixed = getResources().getBoolean(R.bool.isDrawerFixed);

        Toolbar toolbar = findViewById(R.id.toolbar);

        // Navigation Drawer open / close event
        this.drawer = findViewById(R.id.drawer_layout);
        if (!this.isDrawerFixed) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            this.drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        final NavigationView nv = findViewById(R.id.nav_view);
        nv.setNavigationItemSelectedListener(this);

        this.menu = nv.getMenu();

        this.miTrainRequest = menu.findItem(R.id.nav_train_request);
        this.tvUser = nv.getHeaderView(0).findViewById(R.id.tv_hamburger_user);
        this.tvServer = nv.getHeaderView(0).findViewById(R.id.tv_hamburger_server);

        {
            final TextView twVersion = findViewById(R.id.tv_version);
            twVersion.setText("v" + BuildConfig.VERSION_NAME);
        }

        {
            final TextView twBuild = findViewById(R.id.tv_build);
            twBuild.setText(getString(R.string.hamburger_built) + " " + BuildConfig.BUILD_DATETIME);
        }
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /**
     * Hamburger menu click event
     * @param item is menuitem
     * @return true iff the item should be displayed as selected
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_server) {
            this.startActivity(new Intent(this, ServerSelect.class));

        } else if (id == R.id.nav_about) {
            this.startActivity(new Intent(this, About.class));

        } else if (id == R.id.nav_train_request) {
            this.startActivity(new Intent(this, TrainRequest.class));

        } else if (id == R.id.nav_settings) {
            this.startActivity(new Intent(this, Settings.class));

        }

        if (!this.isDrawerFixed)
            this.drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Main menu open / close event.
     * Toggle hamburger in this event.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (this.drawer.isDrawerOpen(GravityCompat.START)) {
                this.drawer.closeDrawer(GravityCompat.START);
            } else {
                this.drawer.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPDisconnectedEvent event) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.sc_disconnected) + "\n" + event.getError())
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {} )
                .show();
        this.updateServer();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(GlobalAuthEvent event) {
        if (event.getParsed().get(4).equalsIgnoreCase("NOT")) {
            // authorization canceled -> disconnect
            new AlertDialog.Builder(this)
                    .setMessage(event.getParsed().get(5))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_ok, (dialog, which) ->
                            TCPClient.getInstance().disconnect("User cancelled")
                    ).show();
        }
        this.updateServer();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokChangeEvent event) {
        this.updateTrainGroup();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokTotalChangeErrorEvent event) {
        int msgId = (event.getTotal()) ? R.string.total_off_error : R.string.total_on_error;
        Toast.makeText(this, getString(msgId, event.getAddr()), Toast.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokAddEvent event) {
        this.updateTrainGroup();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokRemoveEvent event) {
        this.updateTrainGroup();
    }

    @Override
    public void onPause() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        this.updateTrainGroup();
        this.updateServer();
    }

    private void updateTrainGroup() {
        this.menu.removeGroup(R.id.group_train);
        ArrayList<Train> trains = new ArrayList<>(TrainDb.instance.trains.values());
        Collections.sort(trains, (train1, train2) -> train1.addr - train2.addr);

        for (Train t : trains) {
            final MenuItem item = menu.add(R.id.group_train, Menu.NONE, 1, t.getTitle());
            // Set icon
            if (t.total) {
                item.setIcon((t.multitrack) ? R.drawable.ic_train_multi_24dp : R.drawable.ic_train_control_24dp);
            } else {
                item.setIcon((t.stepsSpeed == 0) ? R.drawable.ic_train_stop_24dp : R.drawable.ic_train_speed_24dp);
            }
            item.setOnMenuItemClickListener(item1 -> {
                if (this instanceof TrainHandler) {
                    ((TrainHandler)this).setTrain(t);
                } else {
                    Intent intent = new Intent(this, TrainHandler.class);
                    intent.putExtra("train_addr", t.addr);
                    startActivity(intent);
                }
                return false;
            });
        }
    }

    protected void currentEngineClosed() {
        // Current Engine closed -> open next engine if available
        if (!(this instanceof TrainHandler))
            return;
        ArrayList<Train> trains = new ArrayList<>(TrainDb.instance.trains.values());
        Collections.sort(trains, (train1, train2) -> train1.addr - train2.addr);

        if (!trains.isEmpty()) {
            Intent intent = new Intent(this, TrainHandler.class);
            intent.putExtra("train_addr", trains.get(0).addr);
            startActivity(intent);
        }
    }

    private void updateServer() {
        boolean connected = TCPClient.getInstance().connected();
        this.miTrainRequest.setVisible(connected);
        if (connected) {
            Server server = TCPClient.getInstance().server;
            this.tvUser.setText(server.username);
            this.tvServer.setText(!server.name.isEmpty() ? server.name : server.host);
        } else {
            this.tvUser.setText(R.string.hamburger_state_unauthenticated);
            this.tvServer.setText(R.string.hamburger_state_disconnected);
        }
    }

}
