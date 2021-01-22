package cz.mendelu.xmarik.train_manager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import cz.mendelu.xmarik.train_manager.R;


public class ServerSelect extends NavigationBase {

    private static final int NUM_PAGES = 2;

    TabAdapter tabAdapter;
    FloatingActionButton flbAdd;
    FloatingActionButton flbRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_server_select);
        super.onCreate(savedInstanceState);

        flbAdd = findViewById(R.id.flbAdd);
        flbRefresh = findViewById(R.id.flbRefresh);

        // set toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.activity_server_select_title));
        setSupportActionBar(toolbar);

        // set tabs
        ViewPager2 viewPager = findViewById(R.id.servers_pager);
        TabLayout tabLayout = findViewById(R.id.servers_tabs);
        tabAdapter = new TabAdapter(this);
        viewPager.setAdapter(tabAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText(getString(R.string.conn_servers_found));
            else tab.setText(getString(R.string.conn_servers_stored));
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    flbAdd.hide();
                    flbRefresh.show();
                }
                else {
                    flbRefresh.hide();
                    flbAdd.show();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void discoverServers(View view) {
        Fragment fragment = tabAdapter.getFragment("0");
        if (fragment instanceof ServerSelectFound) ((ServerSelectFound) fragment).discoverServers();
    }

    public void addServer(View view) {
        startActivity(new Intent(view.getContext(), ServerEdit.class));
    }

    private class TabAdapter extends FragmentStateAdapter {

        public TabAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return (position == 0) ? new ServerSelectFound() : new ServerSelectStored();
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }

        public Fragment getFragment(String index) {
            return getSupportFragmentManager().findFragmentByTag("f" + index);
        }

    }
}