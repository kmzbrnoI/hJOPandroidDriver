package cz.mendelu.xmarik.train_manager.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;

import cz.mendelu.xmarik.train_manager.BuildConfig;
import cz.mendelu.xmarik.train_manager.R;

public class About extends NavigationBase {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_about);
        super.onCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        {
            final TextView twVersion = findViewById(R.id.tv_app_version);
            twVersion.setText("v" + BuildConfig.VERSION_NAME);
        }
        {
            final TextView twBuild = findViewById(R.id.tv_app_build);
            twBuild.setText(getString(R.string.hamburger_built) + " " + BuildConfig.BUILD_DATETIME);
        }
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
}
