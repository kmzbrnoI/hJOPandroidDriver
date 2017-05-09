package cz.mendelu.xmarik.train_manager;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;

import cz.mendelu.xmarik.train_manager.events.CriticalErrorEvent;

public class About extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //noinspection SimplifiableIfStatement
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_server) {
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_train_manage) {
            if(ServerList.getInstance().getActiveServer() == null) {
                Toast.makeText(getApplicationContext(),
                        R.string.neniServer,
                        Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(this, TrainHandler.class);
                startActivity(intent);
            }
        } else if (id == R.id.nav_trains) {
            if(ServerList.getInstance().getActiveServer() == null) {
                Toast.makeText(getApplicationContext(),
                        R.string.neniServer,
                        Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(this, TrainRequest.class);
                startActivity(intent);
            }
        } else if (id == R.id.nav_view) {
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
