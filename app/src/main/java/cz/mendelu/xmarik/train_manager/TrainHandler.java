package cz.mendelu.xmarik.train_manager;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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

import cz.mendelu.xmarik.train_manager.events.ReloadEvent;

public class TrainHandler extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ArrayList<String> activeTrains;
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

    String err;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_handler);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        activeTrains = new ArrayList<String>();
        managed = new ArrayList<>();
        context = this;
        err = null;

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
        this.clicableManager(true,false);

        final ArrayAdapter<String> funcAdapter1 = new ArrayAdapter<String>(this,
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

            ArrayAdapter<String> lAdapter = new ArrayAdapter<String>(this,
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

            checkBoxView1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {

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

            spinner1.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                    // ListView Clicked item index
                    int itemPosition = position;

                    // ListView Clicked item value
                    Train t =  activeServer.getTrain(spinner1.getItemAtPosition(position).toString());
                    String itemValue = t.getName();
                    if (itemValue != null) {
                        train1 = activeServer.getTrain(itemValue);
                        active1 = itemValue;
                        name1.setText(train1.getName());
                        speed1.setProgress(train1.getSpeed());
                        direction1.setChecked(train1.isDirection());

                        if (direction1.isChecked()) {
                            direction1.setText("vpřed");
                        } else direction1.setText("vzad");

                        group1.setChecked(train1.isControled());
                        kmhSpeed1.setText(Double.toString(train1.getKmhSpeed()) + "km/h");
                        totalManaged.setChecked(train1.getTotalManaged());

                        syncStatus(train1,status1);

                        if (train1.getTotalManaged()) {
                            clicableManager(true, true);
                        } else {
                            clicableManager(true, false);
                        }

                        //set custom adapter with check boxes to list view
                        CheckBoxAdapter dataAdapter = new CheckBoxAdapter(context,
                                R.layout.trainfunctioninfo, new ArrayList<TrainFunction>(Arrays.asList(train1.getFunction())));
                        checkBoxView1.setAdapter(dataAdapter);

                    }


                }

            });

            group1.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){

                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(train1 != null)
                        if(b)
                        {
                            managed.add(train1);
                            train1.setControled(true);
                        }else
                        {
                            train1.setControled(false);
                            managed.remove(train1);
                        }
                }
            });

            totalManaged.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (train1 != null) {
                        String msg = train1.setTotalManged(b);
                        sendNext(msg);
                    }
                }
            });

            speed1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    if(train1 != null){
                        train1.setSpeed(progress);
                        String text = train1.GetSpeedTxt();
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
                        train1.setSpeed(progress);
                        String text = train1.GetSpeedTxt();
                        setSpeed(progress, train1);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                totalManaged2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (train1 != null) {
                            String msg = train1.setTotalManged(b);
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
                        int itemPosition = position;


                        Train t =  activeServer.getTrain(spinner2.getItemAtPosition(position).toString());
                        String itemValue = t.getName();
                        if (itemValue != null) {
                            train2 = activeServer.getTrain(itemValue);
                            active2 = itemValue;
                            name2.setText(train2.getName());
                            speed2.setProgress(train2.getSpeed());
                            direction2.setChecked(train2.isDirection());

                            if (direction2.isChecked()) {
                                direction2.setText("vpřed");
                            } else direction2.setText("vzad");

                            group2.setChecked(train2.isControled());
                            kmhSpeed2.setText(Double.toString(train2.getKmhSpeed()) + "km/h");
                            totalManaged.setChecked(train2.getTotalManaged());

                            if (train2.getTotalManaged()) {
                                clicableManager(false, true);
                            } else {
                                clicableManager(false, false);
                            }

                            syncStatus(train2,status2);

                            //set custom adapter with check boxes to list view
                            CheckBoxAdapter dataAdapter = new CheckBoxAdapter(context,
                                    R.layout.trainfunctioninfo, new ArrayList<TrainFunction>(Arrays.asList(train2.getFunction())));
                            checkBoxView2.setAdapter(dataAdapter);

                        }
                    }
                });

                group2.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){

                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(train2 != null)
                            if(b)
                            {
                                managed.add(train2);
                                train2.setControled(true);
                            }else
                            {
                                train2.setControled(false);
                                managed.remove(train2);
                            }
                    }
                });
            }
        } else Toast.makeText(getApplicationContext(),
                "Žádný server není authorizován", Toast.LENGTH_LONG)
                .show();
    }

    public void addTrain(String adr) {
        this.activeTrains.add(adr);
    }

    public void removeTrain(String adr) {
        this.activeTrains.remove(adr);
    }

    public void setSpeed(int spd, Train train) {

        int speed = spd;

        if (train != null) {
            if (managed.contains(train)) {
                for (Train s : managed) {
                    Train t = s;
                    t.setSpeed(speed);
                    String text = t.GetSpeedSTxt();
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
                    Train t = s;
                    String text = t.emergencyStop();
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
                    Train t = s;
                    String text = t.emergencyStop();
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

    public void directionChange1(View view) {
        Server tmp = ServerList.getInstance().getActiveServer();

        if (direction1.isChecked()) {
            direction1.setText("vpřed");
        } else direction1.setText("vzad");


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

    public void directionChange2(View view) {
        if (direction2.isChecked()) {
            direction2.setText("vpřed");
        } else direction2.setText("vzad");

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

        if (train1 != null) {
            train1 = activeServer.getTrain(train1.getName());
            name1.setText(train1.getName());
            speed1.setProgress(train1.getSpeed());
            direction1.setChecked(train1.isDirection());
            group1.setChecked(managed.contains(train1));
            kmhSpeed1.setText(Double.toString(train1.getKmhSpeed())+ "km/h");

            syncStatus(train1,status1);
            clicableManager(true, train1.getTotalManaged());
        }

        if (landscape) {
            if(train2 != null){
                train2 = activeServer.getTrain(train2.getName());
                name2.setText(train2.getName());
                speed2.setProgress(train2.getSpeed());
                direction2.setChecked(train2.isDirection());
                group2.setChecked(managed.contains(train2));
                kmhSpeed2.setText(Double.toString(train2.getKmhSpeed()));

                syncStatus(train2,status2);
            }
        }


    }

    private void syncStatus(Train t, ImageButton s)
    {
        if (t.getErr() != null) {
            if (t.getErr().equals("stolen")) {
                s.setImageResource(R.mipmap.ic_yellow);
            } else s.setImageResource(R.mipmap.ic_red);
        } else s.setImageResource(R.mipmap.ic_green);
    }


    public void showStats1(View view) {

        if (train1.getErr() != null) {
            if (train1.getErr().equals("stolen")) {
                sendNext(train1.getBase()+";PLEASE");
            } else {
                Toast.makeText(getApplicationContext(),
                        train1.getErr(),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            message = train1.getUserTrainInfo();

            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_LONG).show();
        }
    }

    public void showStats2(View view) {

        if (train2.getErr() != null) {
            if (train2.getErr().equals("stolen")) {
                sendNext(train2.getBase()+";PLEASE");
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

        //sends the message to the server
        if (TCPClientApplication.getInstance().getClient() != null) {
            TCPClientApplication.getInstance().getClient().sendMessage(message);
            Log.e("data", "C: Odeslana zpráva: " + message + "");
        } else {
            //TODO error
        }

    }

    @Subscribe
    public void onEvent(ReloadEvent event) {
        Log.e("", "handler reload");
        dataChangeNotify();
    }

    private void clicableManager(boolean firstTrain, boolean state) {
        if (firstTrain) {
            direction1.setEnabled(state);
            speed1.setEnabled(state);
            stopButton1.setEnabled(state);
            idleButton1.setEnabled(state);
            group1.setEnabled(state);
            speed1.setEnabled(state);

            if(!state)
            {
                if(group1.isChecked())
                {
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
            group2.setEnabled(state);

            if(!state)
            {
                if(group2.isChecked())
                {
                    group2.setChecked(false);
                    managed.remove(train2);
                }
            }
        }


    }

    public void stopTrains(){

        if (train1 != null) {
            setSpeed(0, train1);
        }
        if (train2 != null) {
            setSpeed(0, train2);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.train_handler, menu);
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
            Intent intent = new Intent(this, Servers.class);
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
            Intent intent = new Intent(this, Servers.class);
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
    public void onDestroy(){
        // set trains speed to 0
        stopTrains();
        super.onDestroy();
    }
}
