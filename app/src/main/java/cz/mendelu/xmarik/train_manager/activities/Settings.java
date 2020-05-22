package cz.mendelu.xmarik.train_manager.activities;

import android.os.Bundle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import android.widget.CompoundButton;
import android.widget.Switch;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.storage.SettingsDb;

public class Settings extends NavigationBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_settings);
        super.onCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Switch volume = findViewById(R.id.sVolumeSpeed);
        volume.setChecked(SettingsDb.instance.getSpeedVolume());
        volume.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsDb.instance.setSpeedVolume(isChecked);
            }
        });

        Switch onlyAvailableFunctions = findViewById(R.id.sOnlyAvailableFunctions);
        onlyAvailableFunctions.setChecked(SettingsDb.instance.getOnlyAvailableFunctions());
        onlyAvailableFunctions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsDb.instance.setOnlyAvailableFunctions(isChecked);
            }
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

}
