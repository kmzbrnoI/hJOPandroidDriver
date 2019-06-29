package cz.mendelu.xmarik.train_manager.storage;

import android.content.SharedPreferences;

/**
 * Storage of global settings.
 */

public class SettingsDb {
    public static SettingsDb instance;
    SharedPreferences preferences;
    boolean speed_volume;
    boolean only_avaiable_functions;

    public SettingsDb(SharedPreferences preferences) {
        this.preferences = preferences;
        this.loadSettings();
    }

    public void loadSettings() {
        speed_volume = preferences.getBoolean("SpeedVolume", false);
        only_avaiable_functions = preferences.getBoolean("OnlyAvailableFunctions", true);
    }

    public boolean getSpeedVolume() {
        return speed_volume;
    }

    public void setSpeedVolume(boolean sv) {
        speed_volume = sv;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("SpeedVolume", sv);
        editor.commit();
    }

    public boolean getOnlyAvailableFunctions() {
        return only_avaiable_functions;
    }

    public void setOnlyAvailableFunctions(boolean oaf) {
        only_avaiable_functions = oaf;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("OnlyAvailableFunctions", oaf);
        editor.commit();
    }

}
