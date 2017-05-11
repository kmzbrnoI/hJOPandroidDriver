package cz.mendelu.xmarik.train_manager.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.ServerList;
import cz.mendelu.xmarik.train_manager.events.CriticalErrorEvent;

public class About extends NavigationBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_about);
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
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
    public void onResume() {
        super.onResume();
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

    public void linkhJOP(View v) {
        Uri uriUrl = Uri.parse("http://hjop.kmz-brno.cz/");
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }
}
