package cz.mendelu.xmarik.train_manager.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.ServerList;

/**
 * Class NavigationBase implements base class for all activities, which want to have navigation
 * toolbar.
 * Created by Jan Horacek
 */

public class NavigationBase extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Context context = this.getApplicationContext();

        // Navigation Drawer open / close event
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);
        nv.setNavigationItemSelectedListener(this);

        // add version number to hamburger_header
        try {
            TextView tw = ((TextView) nv.getHeaderView(0).findViewById(R.id.tv_version));
            tw.setText("v" + context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Version", "App version exception!", e);
        }
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
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent);
        } else if (id == R.id.nav_train_manage) {
            if(ServerList.getInstance().getActiveServer() == null) {
                Toast.makeText(getApplicationContext(),
                        R.string.conn_no_server_authorized,
                        Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(this, TrainHandler.class);
                startActivity(intent);
            }
        } else if (id == R.id.nav_trains) {
            if(ServerList.getInstance().getActiveServer() == null) {
                Toast.makeText(getApplicationContext(),
                        R.string.conn_no_server_authorized,
                        Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(this, TrainRequest.class);
                startActivity(intent);
            }
        }else if (id == R.id.nav_ack_trains) {
            if(ServerList.getInstance().getActiveServer() == null) {
                Toast.makeText(getApplicationContext(),
                        R.string.conn_no_server_authorized,
                        Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(this, TrainRelease.class);
                startActivity(intent);
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Main menu open / close event.
     * Toggle hamburger in this event.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);

        switch (item.getItemId()) {
            case android.R.id.home:
                if(drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                else {
                    drawer.openDrawer(GravityCompat.START);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
