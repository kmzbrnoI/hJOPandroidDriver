package cz.mendelu.xmarik.train_manager.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import cz.mendelu.xmarik.train_manager.BuildConfig;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.GlobalAuthEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectEvent;
import cz.mendelu.xmarik.train_manager.network.TCPClientApplication;

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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);

        // Navigation Drawer open / close event
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView nv = findViewById(R.id.nav_view);
        nv.setNavigationItemSelectedListener(this);

        // add version number to hamburger_header
        TextView tw = nv.getHeaderView(0).findViewById(R.id.tv_version);
        tw.setText(String.format("v%s", BuildConfig.VERSION_NAME));
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
            startActivity(new Intent(this, ServerSelect.class));

        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, About.class));

        } else if (id == R.id.nav_train_control) {
            startActivity(new Intent(this, TrainHandler.class));

        } else if (id == R.id.nav_train_request) {
            startActivity(new Intent(this, TrainRequest.class));

        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, Settings.class));

        } else if (id == R.id.nav_train_release) {
            startActivity(new Intent(this, TrainRelease.class));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Main menu open / close event.
     * Toggle hamburger in this event.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPDisconnectEvent event) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.disconnected))
                .setCancelable(false)
                .setPositiveButton("ok", (dialog, which) -> {} )
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(GlobalAuthEvent event) {
        if (event.getParsed().get(4).toUpperCase().equals("NOT")) {
            // authorization canceled -> disconnect
            new AlertDialog.Builder(this)
                    .setMessage(event.getParsed().get(5))
                    .setCancelable(false)
                    .setPositiveButton("ok", (dialog, which) ->
                            TCPClientApplication.getInstance().disconnect()
                    ).show();
        }
    }

    @Override
    public void onStop() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onStart() {
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        super.onStart();
    }

}
