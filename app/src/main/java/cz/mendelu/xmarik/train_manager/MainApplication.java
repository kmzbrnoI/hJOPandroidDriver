package cz.mendelu.xmarik.train_manager;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int mode = getTheme(sharedPreferences.getString("theme", ""));
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Convert string value of theme mode into int used by AppCompatDelegate.setDefaultNightMode()
     * @param value Name of mode
     * @return Code number of mode
     */
    public static int getTheme(String value) {
        switch (value) {
            case "MODE_NIGHT_NO":
                return AppCompatDelegate.MODE_NIGHT_NO;
            case "MODE_NIGHT_YES":
                return AppCompatDelegate.MODE_NIGHT_YES;
            case "MODE_NIGHT_AUTO_BATTERY":
                return AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}
