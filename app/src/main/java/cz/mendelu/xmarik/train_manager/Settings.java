package cz.mendelu.xmarik.train_manager;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static final String MyPREFERENCES = "settings";
    public static SharedPreferences sharedpreferences;
    static int buttons = 3;
    static boolean auto = true;
    static boolean allF = true;
    RadioGroup funcName;
    int funcNumber;
    CheckBox allFunc;
    CheckBox automatic;

    public static void loadData(SharedPreferences s) {
        sharedpreferences = s;
        //TODO nejeaka chyby java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Integer
      /*  if (s.contains("radio")) {
            buttons = s.getInt("radio",3);
        }
        if (s.contains("auto")) {
            auto = s.getBoolean("auto", true);
        }
        if (s.contains("allFunc")) {
            allF = s.getBoolean("allFunc", true);
        }*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        funcName = (RadioGroup) findViewById(R.id.funcGroup);
        allFunc = (CheckBox) findViewById(R.id.allFunc);
        automatic = (CheckBox) findViewById(R.id.auto);
        //ReadBtn();
        funcName.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                                                @Override
                                                public void onCheckedChanged(RadioGroup group, int checkedId) {

                                                    if (checkedId == R.id.numbers) {
                                                        funcNumber = 1;
                                                        buttons = 1;

                                                    } else if (checkedId == R.id.names) {
                                                        funcNumber = 2;
                                                        buttons = 2;
                                                    } else {
                                                        funcNumber = 3;
                                                        buttons = 3;
                                                    }
                                                }
                                            }
        );
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public void save(View view) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("radio", "" + funcNumber);
        editor.putString("auto", automatic.isChecked() ? "0" : "1");
        editor.putString("allFunc", automatic.isChecked() ? "0" : "1");
        editor.commit();
    }

    public void deleteAllPrivate(View view) {
        ServerList l = ServerList.getInstance();
        for (Server s : l.getCustomServers()) {
            s.setUserName(null);
            s.setUserPassword(null);
        }
        Toast.makeText(getApplicationContext(),
                "hesla vymazána",
                Toast.LENGTH_LONG).show();
    }

    public void deleteAllServers(View view) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.remove("servers");
        editor.clear();
        editor.commit();
        Toast.makeText(getApplicationContext(),
                "servery byly smazány",
                Toast.LENGTH_LONG).show();
    }

    private void showDialog(final boolean all) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.delete_dialog);
        dialog.setTitle("Smazání údajů");
        //set dialog component
        final TextView mName = (TextView) findViewById(R.id.deleteText);
        if (all) {
            mName.setText("opravdu chce vymazat všechny uložené údaje?");
        } else mName.setText("opravdu chce vymazat všechna uložená jména a hesla?");
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOk);
        Button dialogButtonC = (Button) dialog.findViewById(R.id.dialogButtonCancel);
        // if button is clicked, close the custom dialog
        dialogButtonC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (all) {
                    ServerList.getInstance().deleteAllData();
                } else ServerList.getInstance().deleteAllUserData();
                dialog.dismiss();
            }
        });
        dialog.show();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_server) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent);

        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);

        } else if (id == R.id.nav_train_manage) {
            Intent intent = new Intent(this, TrainHandler.class);
            startActivity(intent);

        } else if (id == R.id.nav_trains) {
            Intent intent = new Intent(this, Trains_box.class);
            startActivity(intent);

        } else if (id == R.id.nav_view) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
