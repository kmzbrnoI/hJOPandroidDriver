package cz.mendelu.xmarik.train_manager.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import cz.mendelu.xmarik.train_manager.MainApplication;
import cz.mendelu.xmarik.train_manager.R;

public class Settings extends NavigationBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_settings);
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_settings, new SettingsFragment())
                .commit()
        ;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            ListPreference theme = getPreferenceScreen().findPreference("theme");
            if (theme != null) {
                theme.setOnPreferenceChangeListener((preference, value) -> {
                    int mode = MainApplication.getTheme(value.toString());
                    AppCompatDelegate.setDefaultNightMode(mode);
                    return true;
                });
            }
        }
    }

}
