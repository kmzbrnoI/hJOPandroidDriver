package cz.mendelu.xmarik.train_manager.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.mendelu.xmarik.train_manager.adapters.CheckBoxAdapter;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.ServerList;
import cz.mendelu.xmarik.train_manager.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.models.Train;
import cz.mendelu.xmarik.train_manager.events.CriticalErrorEvent;
import cz.mendelu.xmarik.train_manager.events.ReloadEvent;

public class TrainHandler extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    String err;
    //private ArrayList<String> activeTrains;
    private Server activeServer;
    private List<String> array;
    private List<Train> managed;
    private TextView name1;
    private SeekBar speed1;
    private Switch direction1;
    private CheckBox totalManaged;
    private Spinner spinner1;
    private Button stopButton1;
    private CheckBox group1;
    private boolean landscape = false;
    private ImageButton status1;
    private Train train1;
    private ListView checkBoxView1;
    private TextView kmhSpeed1;
    private Button idleButton1;
    private String message;
    private Train train2;
    private TextView name2;
    private SeekBar speed2;
    private Switch direction2;
    private CheckBox group2;
    private ImageButton status2;
    private String active1, active2;
    private TextView kmhSpeed2;
    private Button idleButton2;
    private Button stopButton2;
    private CheckBox totalManaged2;
    private ListView checkBoxView2;
    private Spinner spinner2;
    private Context context;
    private boolean update;
    private Long timer;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_handler);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        update = false;

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //activeTrains = new ArrayList<>();
        managed = new ArrayList<>();
        context = this;
        err = null;
        timer = System.currentTimeMillis();

        name1 = (TextView) findViewById(R.id.handlerName1);
        speed1 = (SeekBar) findViewById(R.id.speedkBar1);
        direction1 = (Switch) findViewById(R.id.handlerDirection1);
        idleButton1 = (Button) findViewById(R.id.startButton1);
        stopButton1 = (Button) findViewById(R.id.stopButton1);
        group1 = (CheckBox) findViewById(R.id.goupManaged1);
        status1 = (ImageButton) findViewById(R.id.imageButton1);
        checkBoxView1 = (ListView) findViewById(R.id.checkBoxView1);
        kmhSpeed1 = (TextView) findViewById(R.id.kmh1);
        totalManaged = (CheckBox) findViewById(R.id.totalManaged);
        EventBus.getDefault().register(this);
        active1 = "";
        active2 = "";

        //makes all controls inactive before train is chosen
        this.clickableManager(true, false);

        final ArrayAdapter<String> funcAdapter1 = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ServerList.FUNCTION);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscape = true;

            name2 = (TextView) findViewById(R.id.handlerName2);
            speed2 = (SeekBar) findViewById(R.id.speedkBar2);
            direction2 = (Switch) findViewById(R.id.handlerDirection2);
            group2 = (CheckBox) findViewById(R.id.goupManaged2);
            status2 = (ImageButton) findViewById(R.id.imageButton2);
            kmhSpeed2 = (TextView) findViewById(R.id.kmh2);
            idleButton2 = (Button) findViewById(R.id.startButton2);
            stopButton2 = (Button) findViewById(R.id.stopButton2);
            totalManaged2 = (CheckBox) findViewById(R.id.totalManaged2);
            checkBoxView2 = (ListView) findViewById(R.id.checkBoxView2);
            spinner2 = (Spinner) findViewById(R.id.spinner2);

        }

        activeServer = ServerList.getInstance().getActiveServer();

        if (activeServer != null) {
            spinner1 = (Spinner) findViewById(R.id.spinner1);
            array = activeServer.getTrainString();

            ArrayAdapter<String> lAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, array);

            spinner1.setAdapter(lAdapter);

            checkBoxView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // When clicked, show a toast with the TextView text

                    train1.chageFunc(position);
                    String message = train1.getFunctionStr(0, train1.getFunction().length - 1);
                    //zmena zaskrknutí i pri kliknuti mimo checkbox
                    checkBoxView1.setItemChecked(position, !checkBoxView1.isItemChecked(position));

                    sendNext(message);

                }
            });

            checkBoxView1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                    train1.chageFunc(i);
                    String message = train1.getFunctionStr(0, train1.getFunction().length - 1);

                    sendNext(message);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            direction1.setOnClickListener(new CompoundButton.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!update) {
                        if (direction1.isChecked()) {
                            direction1.setText(R.string.ta_direction_forward);
                        } else direction1.setText(R.string.ta_direction_backwards);
                        if (managed.contains(train1)) {
                            for (Train s : managed) {
                                String text = s.changeDirection();
                                sendNext(text);
                            }
                        } else {
                            if (train1 != null) {
                                String text = train1.changeDirection();
                                sendNext(text);
                            }
                        }
                    }
                }
            });

            direction1.setOnGenericMotionListener(new CompoundButton.OnGenericMotionListener() {
                @Override
                public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                    if (!update) {
                        if (direction1.isChecked()) {
                            direction1.setText(R.string.ta_direction_forward);
                        } else direction1.setText(R.string.ta_direction_backwards);
                        if (managed.contains(train1)) {
                            for (Train s : managed) {
                                String text = s.changeDirection();
                                sendNext(text);
                            }
                        } else {
                            if (train1 != null) {
                                String text = train1.changeDirection();
                                sendNext(text);
                            }
                        }
                    }
                    return false;
                }

            });

            spinner1.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    // ListView Clicked item value
                    String value = spinner1.getItemAtPosition(position).toString();
                    if (spinner1.getCount() > 0 && value.contains(":")) {
                        String lokoName = value.substring(0, value.indexOf(":"));
                        Train t = activeServer.getTrain(lokoName);
                        String itemValue = t != null ? t.getName() : null;
                        if (itemValue != null) {
                            train1 = activeServer.getTrain(itemValue);
                            active1 = itemValue;
                            name1.setText(train1.getDisplayLokoName());
                            speed1.setProgress(train1.getSpeed());
                            direction1.setChecked(train1.isDirection());
                            if (direction1.isChecked()) {
                                direction1.setText(R.string.ta_direction_forward);
                            } else direction1.setText(R.string.ta_direction_backwards);
                            group1.setChecked(train1.isControled());
                            kmhSpeed1.setText(String.format("%s km/h", Integer.toString(train1.getKmhSpeed())));
                            totalManaged.setChecked(train1.getTotalManaged());
                            syncStatus(train1, status1);

                            if (train1.getTotalManaged()) {
                                clickableManager(true, true);
                            } else {
                                clickableManager(true, false);
                            }
                            //set custom adapter with check boxes to list view
                            CheckBoxAdapter dataAdapter = new CheckBoxAdapter(context,
                                    R.layout.trainfunctioninfo, new ArrayList<>(Arrays.asList(train1.getFunction())));
                            checkBoxView1.setAdapter(dataAdapter);

                        }
                    }
                }

            });

            if (array.size() <= 1) {
                group1.setEnabled(false);
            } else group1.setEnabled(true);

            group1.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (train1 != null)
                        if (b) {
                            if (!managed.contains(train1)) {
                                managed.add(train1);
                                train1.setControled(true);
                            }
                        } else {
                            train1.setControled(false);
                            group1.setChecked(false);
                            managed.remove(train1);
                        }
                }
            });

            totalManaged.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (train1 != null) {
                        String msg = train1.setTotalManged(b);
                        if (!b) {
                            group1.setChecked(false);
                        }
                        sendNext(msg);
                    }
                }
            });

            speed1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    if (timer < System.currentTimeMillis())
                        if (train1 != null) {
                            //because server cant handle as mutch packet as client possible could send
                            timer = System.currentTimeMillis() + 200;
                            train1.setSpeed(progress);
                            setSpeed(progress, train1);
                        }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            if (landscape) {
                spinner2.setAdapter(lAdapter);
                speed2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        if (timer < System.currentTimeMillis())
                            if (train2 != null) {
                                //because server cant handle as mutch packet as client possible could send
                                timer = System.currentTimeMillis() + 200;
                                train2.setSpeed(progress);
                                setSpeed(progress, train2);
                            }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                if (array.size() <= 1) {
                    group2.setEnabled(false);
                } else group2.setEnabled(true);

                direction2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        setDirectionText(2);
                        if (!update) {
                            if (managed.contains(train2)) {
                                for (Train s : managed) {
                                    String text = s.changeDirection();
                                    sendNext(text);
                                }
                            } else {
                                if (train2 != null) {
                                    String text = train2.changeDirection();
                                    sendNext(text);
                                }
                            }
                        }
                    }
                });

                totalManaged2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (train1 != null) {
                            String msg = train1.setTotalManged(b);
                            if (!b) {
                                group2.setChecked(false);
                            }
                            sendNext(msg);
                        }
                    }
                });

                spinner2.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }

                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        // ListView Clicked item index
                        String value = spinner2.getItemAtPosition(position).toString();
                        String lokoName = value.substring(0, value.indexOf(":"));
                        Train t = activeServer.getTrain(lokoName);
                        String itemValue = t != null ? t.getName() : null;
                        if (itemValue != null) {
                            train2 = activeServer.getTrain(itemValue);
                            active2 = itemValue;
                            name2.setText(train2.getDisplayLokoName());
                            speed2.setProgress(train2.getSpeed());
                            direction2.setChecked(train2.isDirection());

                            if (direction2.isChecked()) {
                                direction2.setText(R.string.ta_direction_forward);
                            } else direction2.setText(R.string.ta_direction_backwards);

                            group2.setChecked(train2.isControled());
                            kmhSpeed2.setText(String.format("%s km/h", Integer.toString(train2.getKmhSpeed())));
                            totalManaged.setChecked(train2.getTotalManaged());

                            if (train2.getTotalManaged()) {
                                clickableManager(false, true);
                            } else {
                                clickableManager(false, false);
                            }

                            syncStatus(train2, status2);

                            //set custom adapter with check boxes to list view
                            CheckBoxAdapter dataAdapter = new CheckBoxAdapter(context,
                                    R.layout.trainfunctioninfo, new ArrayList<>(Arrays.asList(train2.getFunction())));
                            checkBoxView2.setAdapter(dataAdapter);
                        }
                    }
                });

                group2.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (train2 != null)
                            if (b) {
                                if (!managed.contains(train2)) {
                                    managed.add(train2);
                                    train2.setControled(true);
                                }
                            } else {
                                train2.setControled(false);
                                managed.remove(train2);
                            }
                    }
                });
            }
        } else Toast.makeText(getApplicationContext(),
                R.string.conn_no_server_authorized, Toast.LENGTH_LONG)
                .show();
    }

    private void setDirectionText(int direction) {
        if (!update)
            if (direction == 1) {
                if (direction1.isChecked()) {
                    direction1.setText(R.string.ta_direction_forward);
                } else direction1.setText(R.string.ta_direction_backwards);
            } else {
                if (direction2.isChecked()) {
                    direction2.setText(R.string.ta_direction_forward);
                } else direction2.setText(R.string.ta_direction_backwards);
            }
    }

    public void setSpeed(int spd, Train train) {

        if (train != null && !update) {
            if (managed.contains(train)) {
                for (Train s : managed) {
                    s.setSpeed(spd);
                    String text = s.GetSpeedSTxt();
                    sendNext(text);
                }
            } else {
                if (train.GetSpeedSTxt() != null) {
                    String text = train.GetSpeedSTxt();
                    sendNext(text);
                }
            }
        }
    }

    public void stop1(View view) {
        speed1.setProgress(0);
        if (train1 != null)
            if (managed.contains(train1)) {
                for (Train s : managed) {
                    String text = s.emergencyStop();
                    sendNext(text);
                }
            } else {
                String text = train1.emergencyStop();
                sendNext(text);
            }
    }

    public void stop2(View view) {
        speed2.setProgress(0);
        if (train2 != null)
            if (managed.contains(train2)) {
                for (Train s : managed) {
                    String text = s.emergencyStop();
                    sendNext(text);
                }
            } else {
                String text = train2.emergencyStop();
                sendNext(text);
            }
    }

    public void idle1(View view) {
        speed1.setProgress(0);
        setSpeed(0, train1);
    }

    public void idle2(View view) {
        speed2.setProgress(0);
        setSpeed(0, train2);
    }

    public void changeManaged(String s) {
        Server tmp = ServerList.getInstance().getActiveServer();
        Train t = tmp.getTrain(s);
        String text = null;

        if (managed.contains(t)) {
            this.managed.remove(t);
        } else this.managed.add(t);

        if (t.isControled()) {
            text = t.setTotalManged(false);
        } else {
            text = t.setTotalManged(true);
        }
        sendNext(text);
    }

    public void setStatus(String trainAdr, boolean status, String err) {
        Server tmp = ServerList.getInstance().getActiveServer();
        Train t = tmp.getTrain(trainAdr);
        t.setErr(err);
        t.statusOk = status;

        if (!active1.equals("") && active1.equals(trainAdr))
            if (status) {
                status1.setImageResource(R.mipmap.ic_green);
            } else status1.setImageResource(R.mipmap.ic_red);

        if (!active2.equals("") && active2.equals(trainAdr))
            if (status) {
                status2.setImageResource(R.mipmap.ic_green);
            } else status2.setImageResource(R.mipmap.ic_red);
    }

    public void dataChangeNotify() {
        update = true;
        if (train1 != null) {
            train1 = activeServer.getTrain(train1.getName());
            name1.setText(train1.getDisplayLokoName());
            if (train1.getSpeed() != speed1.getProgress()) speed1.setProgress(train1.getSpeed());
            direction1.setChecked(train1.isDirection());
            group1.setChecked(managed.contains(train1));
            kmhSpeed1.setText(String.format("%s km/h", Integer.toString(train1.getKmhSpeed())));
            totalManaged.setChecked(train1.getTotalManaged());

            setDirectionText(1);
            syncStatus(train1, status1);
            clickableManager(true, train1.getTotalManaged());
        }

        if (landscape) {
            if (train2 != null) {
                train2 = activeServer.getTrain(train2.getName());
                name2.setText(train2.getDisplayLokoName());
                speed2.setProgress(train2.getSpeed());
                direction2.setChecked(train2.isDirection());
                group2.setChecked(managed.contains(train2));
                kmhSpeed2.setText(String.format("%s km/h", Integer.toString(train2.getKmhSpeed())));
                totalManaged2.setChecked(train2.getTotalManaged());

                setDirectionText(2);
                syncStatus(train2, status2);
            }
        }
        update = false;
    }

    private void syncStatus(Train t, ImageButton s) {
        if (t.getErr() != null) {
            if (t.getErr().equals("stolen")) {
                s.setImageResource(R.mipmap.ic_yellow);
            } else s.setImageResource(R.mipmap.ic_red);
        } else s.setImageResource(R.mipmap.ic_green);
    }


    public void showStats1(View view) {

        if (train1.getErr() != null) {
            if (train1.getErr().equals("stolen")) {
                sendNext(train1.getBase() + ";PLEASE");
            } else {
                Toast.makeText(getApplicationContext(),
                        train1.getErr(),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            message = train1.getUserTrainInfo();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void showStats2(View view) {

        if (train2.getErr() != null) {
            if (train2.getErr().equals("stolen")) {
                sendNext(train2.getBase() + ";PLEASE");
            } else {
                Toast.makeText(getApplicationContext(),
                        train1.getErr(),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            message = train2.getUserTrainInfo();

            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void sendNext(String message) {

        if (!update) {
            if (TCPClientApplication.getInstance().getClient() != null) {
                TCPClientApplication.getInstance().getClient().sendMessage(message);
                Log.e("data", "C: Odeslana zpráva: " + message + "");
            } else {
                Log.e("data", "C: Neodeslána zpráva: " + message + " Tcp není navázáno");
                //TODO error
            }
        }
    }

    @Subscribe
    public void onEvent(ReloadEvent event) {
        Log.e("", "handler reload");
        dataChangeNotify();
    }

    private void clickableManager(boolean firstTrain, boolean state) {
        if (firstTrain) {
            direction1.setEnabled(state);
            speed1.setEnabled(state);
            stopButton1.setEnabled(state);
            idleButton1.setEnabled(state);
            if (array != null && array.size() <= 1) {
                group1.setEnabled(state);
            } else group1.setEnabled(false);
            speed1.setEnabled(state);

            if (!state) {
                if (group1.isChecked()) {
                    group1.setChecked(false);
                    managed.remove(train1);
                }
            }
        } else {
            direction2.setClickable(state);
            speed2.setClickable(state);
            stopButton2.setClickable(state);
            idleButton2.setClickable(state);
            speed2.setEnabled(state);
            if (array.size() <= 1) {
                group2.setEnabled(state);
            } else group2.setEnabled(false);

            if (!state) {
                if (group2.isChecked()) {
                    group2.setChecked(false);
                    managed.remove(train2);
                }
            }
        }
    }

    public void stopTrains() {

        if (train1 != null) {
            setSpeed(0, train1);
        }
        if (train2 != null) {
            setSpeed(0, train2);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_server) {
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent);

        } else if (id == R.id.nav_train_manage) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

        } else if (id == R.id.nav_trains) {
            Intent intent = new Intent(this, TrainRequest.class);
            startActivity(intent);

        } else if (id == R.id.nav_view) {
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);

        } else if (id == R.id.nav_ack_trains) {
            Intent intent = new Intent(this, TrainRelease.class);
            startActivity(intent);

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        // set trains speed to 0
        stopTrains();
        super.onStop();
    }

    @Override
    protected void onPause() {
        // set trains speed to 0
        stopTrains();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // set trains speed to 0
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            stopTrains();
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (spinner1 != null) {
            array = activeServer.getTrainString();

            ArrayAdapter<String> lAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, array);

            spinner1.setAdapter(lAdapter);
        }
        if (spinner2 != null) {
            array = activeServer.getTrainString();

            ArrayAdapter<String> lAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, array);

            spinner2.setAdapter(lAdapter);
        }
    }

    @Override
    public void onDestroy() {
        // set trains speed to 0
        stopTrains();
        super.onDestroy();
    }

    @Subscribe
    public void criticalError(CriticalErrorEvent event) {
        ServerList.getInstance().deactivateServer();
        if (event.getMessage().startsWith("connection")) {
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(),
                    event.getMessage(),
                    Toast.LENGTH_LONG).show();
            //possibility of another activity, but need additional analyze
            Intent intent = new Intent(this, ServerSelect.class);
            startActivity(intent);
        }
    }


}
