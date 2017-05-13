package cz.mendelu.xmarik.train_manager.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import cz.mendelu.xmarik.train_manager.TrainDb;
import cz.mendelu.xmarik.train_manager.TrainFunction;
import cz.mendelu.xmarik.train_manager.adapters.CheckBoxAdapter;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.LokAddEvent;
import cz.mendelu.xmarik.train_manager.events.LokChangeEvent;
import cz.mendelu.xmarik.train_manager.events.LokRemoveEvent;
import cz.mendelu.xmarik.train_manager.events.LokRespEvent;
import cz.mendelu.xmarik.train_manager.events.ServerReloadEvent;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.ServerList;
import cz.mendelu.xmarik.train_manager.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.models.Train;
import cz.mendelu.xmarik.train_manager.events.CriticalErrorEvent;

public class TrainHandler extends NavigationBase {
    private List<Train> managed;
    private List<Train> multitrack;
    private List<String> managed_str;
    private ArrayAdapter<String> managed_adapter;
    private Train train;
    private boolean updating;
    private Long timer;
    private Context context;

    private TextView tv_name;
    private SeekBar sb_speed;
    private Switch s_direction;
    private CheckBox chb_total;
    private Spinner s_spinner;
    private Button b_stop;
    private Button b_idle;
    private CheckBox chb_group;
    private ImageButton ib_status;
    private ListView chb_functions;
    private TextView tv_kmhSpeed;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_train_handler);
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updating = false;
        context = this;

        managed = new ArrayList<>();
        multitrack = new ArrayList<>();
        timer = System.currentTimeMillis();

        tv_name = (TextView) findViewById(R.id.handlerName1);
        sb_speed = (SeekBar) findViewById(R.id.speedkBar1);
        s_direction = (Switch) findViewById(R.id.handlerDirection1);
        b_idle = (Button) findViewById(R.id.startButton1);
        b_stop = (Button) findViewById(R.id.stopButton1);
        chb_group = (CheckBox) findViewById(R.id.goupManaged1);
        ib_status = (ImageButton) findViewById(R.id.ib_status);
        chb_functions = (ListView) findViewById(R.id.checkBoxView1);
        tv_kmhSpeed = (TextView) findViewById(R.id.kmh1);
        chb_total = (CheckBox) findViewById(R.id.totalManaged);

        EventBus.getDefault().register(this);

        // fill spinner
        s_spinner = (Spinner)findViewById(R.id.spinner1);
        managed_str = new ArrayList<String>();
        managed_adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, managed_str);
        s_spinner.setAdapter(managed_adapter);
        this.fillHVs();

        // fill functions TODO?
        final ArrayAdapter<String> funcAdapter1 = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ServerList.FUNCTION);

        // TODO: one of these:

        chb_functions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (updating) return;
                chb_functions.setItemChecked(position, !chb_functions.isItemChecked(position));
                train.setFunc(position, chb_functions.isItemChecked(position));
            }
        });

        chb_functions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (updating) return;
                train.setFunc(i, chb_functions.isItemChecked(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        s_direction.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDirectionChange(s_direction.isChecked());
            }
        });

        s_direction.setOnGenericMotionListener(new CompoundButton.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                onDirectionChange(s_direction.isChecked());
                return false;
            }

        });

        s_spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (position >= managed.size()) return;
                train = managed.get(position);
                updateGUTtoHV();
            }

        });

        chb_group.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (!multitrack.contains(train))
                        multitrack.add(train);
                } else {
                    if (multitrack.contains(train))
                        multitrack.remove(train);
                }
            }
        });

        chb_total.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                train.setTotal(b);
            }
        });

        sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO: this is really bad solution, it puts the first speed, not the last!
                if (timer < System.currentTimeMillis())
                    timer = System.currentTimeMillis() + 200;
                    train.setSpeedSteps(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void fillHVs() {
        managed_str.clear();
        managed.clear();
        int index = 0;

        s_spinner.setEnabled(TrainDb.instance.trains.size() > 0);
        if (TrainDb.instance.trains.size() == 0)
            managed_str.add("No trains!"); // TODO: string here

        int i = 0;
        for(Train t : TrainDb.instance.trains.values()) {
            managed.add(t);
            managed_str.add(String.valueOf(t.addr));
            if (t == train) index = i;
            i++;
        }

        // update multitraction
        for (i = multitrack.size()-1; i >= 0; i--)
            if (!managed.contains(multitrack.get(i)))
                multitrack.remove(i);

        chb_group.setEnabled(managed.size() >= 2);
        if (managed.size() < 2) chb_group.setChecked(false);

        s_spinner.setSelection(index);
        if (TrainDb.instance.trains.size() > 0)
            train = managed.get(0);
        else
            train = null;

        managed_adapter.notifyDataSetChanged();
        this.updateGUTtoHV();
    }

    private void onDirectionChange(boolean newDir) {
        if (updating) return;

        if (newDir)
            s_direction.setText(R.string.ta_direction_forward);
        else
            s_direction.setText(R.string.ta_direction_backwards);

        train.setDirection(s_direction.isChecked());

        if (managed.contains(train)) {
            for (Train s : managed)
                s.setDirection(newDir);
        } else {
            train.setDirection(newDir);
        }
    }

    private void updateGUTtoHV() {
        chb_group.setEnabled(train != null);
        chb_total.setEnabled(train != null);

        if (train == null) {
            tv_name.setText("");
            sb_speed.setProgress(0);
            s_direction.setChecked(false);
            s_direction.setText("-");

            chb_group.setChecked(false);
            tv_kmhSpeed.setText("- km/h");
            chb_total.setChecked(false);
            //syncStatus(train, status); ????? TODO

            this.setEnabled(false);

            //set custom adapter with check boxes to list view
            CheckBoxAdapter dataAdapter = new CheckBoxAdapter(context,
                    R.layout.trainfunctioninfo, new ArrayList<TrainFunction>());
            chb_functions.setAdapter(dataAdapter);

            ib_status.setImageResource(R.drawable.ic_circle_gray);

        } else {
            tv_name.setText(String.valueOf(train.addr) + " : " + train.name);
            sb_speed.setProgress(train.stepsSpeed);
            s_direction.setChecked(train.direction);
            if (s_direction.isChecked())
                s_direction.setText(R.string.ta_direction_forward);
            else
                s_direction.setText(R.string.ta_direction_backwards);

            chb_group.setChecked(multitrack.contains(train));
            tv_kmhSpeed.setText(String.format("%s km/h", Integer.toString(train.kmphSpeed)));
            chb_total.setChecked(train.total);

            //syncStatus(train, status); ????? TODO

            this.setEnabled(train.total);

            //set custom adapter with check boxes to list view
            CheckBoxAdapter dataAdapter = new CheckBoxAdapter(context,
                    R.layout.trainfunctioninfo, new ArrayList<>(Arrays.asList(train.function)));
            chb_functions.setAdapter(dataAdapter);

            ib_status.setImageResource(R.drawable.ic_circle_green);
        }
    }

    /*private void setDirectionText(int direction) {
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
    }*/

    public void b_stopClick(View view) {
        sb_speed.setProgress(0);
        if (multitrack.contains(train)) {
            for (Train t : multitrack)
                t.emergencyStop();
        } else {
            train.emergencyStop();
        }
    }

    /*public void stop2(View view) {
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
    }*/

    public void b_idleClick(View view) {
        sb_speed.setProgress(0);
        if (multitrack.contains(train)) {
            for (Train t : multitrack)
                t.setSpeedSteps(0);
        } else {
            train.setSpeedSteps(0);
        }
    }

    /*public void idle2(View view) {
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
                status1.setImageResource(R.drawable.ic_circle_green);
            } else status1.setImageResource(R.drawable.ic_circle_red);

        if (!active2.equals("") && active2.equals(trainAdr))
            if (status) {
                status2.setImageResource(R.drawable.ic_circle_green);
            } else status2.setImageResource(R.drawable.ic_circle_red);
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
                s.setImageResource(R.drawable.ic_circle_yellow);
            } else s.setImageResource(R.drawable.ic_circle_red);
        } else s.setImageResource(R.drawable.ic_circle_green);
    }*/


    public void ib_showStatsClick(View view) {

        /*if (train1.getErr() != null) {
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
        }*/
    }

    /*public void showStats2(View view) {

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
            if (TCPClientApplication.getInstance() != null) {
                TCPClientApplication.getInstance().send(message);
                Log.v("data", "C: Odeslana zpráva: " + message + "");
            } else {
                Log.v("data", "C: Neodeslána zpráva: " + message + " Tcp není navázáno");
                //TODO error
            }
        }
    }*/

    @Subscribe
    public void onEvent(LokChangeEvent event) {
        if (train != null && event.getAddr() == train.addr)
            this.updateGUTtoHV();
    }

    @Subscribe
    public void onEvent(LokAddEvent event) {
        this.fillHVs();
    }

    @Subscribe
    public void onEvent(LokRemoveEvent event) {
        this.fillHVs();
    }

    @Subscribe
    public void onEvent(LokRespEvent event) {
        if (train == null) return;

        tv_kmhSpeed.setText(String.format("%s km/h", Integer.toString(train.kmphSpeed)));

        if (event.getParsed().get(4).toUpperCase().equals("OK"))
            ib_status.setImageResource(R.drawable.ic_circle_green);
        else
            ib_status.setImageResource(R.drawable.ic_circle_red);
    }

    private void setEnabled(boolean enabled) {
        s_direction.setEnabled(enabled);
        sb_speed.setEnabled(enabled);
        b_stop.setEnabled(enabled);
        b_idle.setEnabled(enabled);
        chb_group.setEnabled(enabled);
        chb_functions.setEnabled(enabled);
    }



    /*public void stopTrains() {

        if (train1 != null) {
            setSpeed(0, train1);
        }
        if (train2 != null) {
            setSpeed(0, train2);
        }

    }*/

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        // set trains speed to 0
        //stopTrains();
        super.onStop();
    }

    @Override
    protected void onPause() {
        // set trains speed to 0
        //stopTrains();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // set trains speed to 0
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //stopTrains();
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.fillHVs();
    }

    @Override
    public void onDestroy() {
        // set trains speed to 0
        //stopTrains();
        super.onDestroy();
    }

}
