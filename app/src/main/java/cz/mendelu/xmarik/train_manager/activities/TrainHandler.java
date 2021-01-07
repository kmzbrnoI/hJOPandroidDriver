package cz.mendelu.xmarik.train_manager.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cz.kudlav.scomview.ScomView;
import cz.mendelu.xmarik.train_manager.events.DccEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectEvent;
import cz.mendelu.xmarik.train_manager.network.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.storage.TrainDb;
import cz.mendelu.xmarik.train_manager.models.TrainFunction;
import cz.mendelu.xmarik.train_manager.adapters.FunctionCheckBoxAdapter;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.LokAddEvent;
import cz.mendelu.xmarik.train_manager.events.LokChangeEvent;
import cz.mendelu.xmarik.train_manager.events.LokRemoveEvent;
import cz.mendelu.xmarik.train_manager.events.LokRespEvent;
import cz.mendelu.xmarik.train_manager.models.Train;

public class TrainHandler extends NavigationBase {
    private List<Train> managed;
    private List<Train> multitrack;
    private List<String> managed_str;
    private ArrayAdapter<String> managed_adapter;
    private Train train;
    private boolean updating;
    private Context context;
    private boolean confSpeedVolume;
    private boolean confAvailableFunctions;


    private SeekBar sb_speed;
    private SwitchCompat s_direction;
    private CheckBox chb_total;
    private Spinner s_spinner;
    private Button b_stop;
    private Button b_idle;
    private CheckBox chb_group;
    private ImageButton ib_status;
    private ImageButton ib_dcc;
    private ListView lv_functions;
    private TextView tv_kmhSpeed;
    private TextView tv_expSpeed;
    private TextView tv_expSignalBlock;
    private ScomView scom_expSignal;
    private ImageButton ib_release;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!updating && train != null && train.total && !train.stolen && train.stepsSpeed != sb_speed.getProgress()) {
                if (multitrack.contains(train)) {
                    for (Train s : multitrack)
                        if (!s.stolen)
                            s.setSpeedSteps(sb_speed.getProgress());
                } else
                    train.setSpeedSteps(sb_speed.getProgress());
            }
            timerHandler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_train_handler);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updating = false;
        context = this;

        managed = new ArrayList<>();
        multitrack = new ArrayList<>();

        sb_speed = findViewById(R.id.speedkBar1);
        s_direction = findViewById(R.id.handlerDirection1);
        b_idle = findViewById(R.id.startButton1);
        b_stop = findViewById(R.id.stopButton1);
        chb_group = findViewById(R.id.goupManaged1);
        ib_status = findViewById(R.id.ib_status);
        ib_dcc = findViewById(R.id.ib_dcc);
        ib_release = findViewById(R.id.ib_release);
        lv_functions = findViewById(R.id.checkBoxView1);
        tv_kmhSpeed = findViewById(R.id.kmh1);
        tv_expSpeed = findViewById(R.id.expSpeed);
        tv_expSignalBlock = findViewById(R.id.expSignalBlock);
        scom_expSignal = findViewById(R.id.scom_view);
        chb_total = findViewById(R.id.totalManaged);

        // fill spinner
        s_spinner = findViewById(R.id.spinner1);
        managed_str = new ArrayList<>();
        managed_adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, managed_str);
        s_spinner.setAdapter(managed_adapter);
        this.fillHVs();

        // GUI events:
        s_direction.setOnCheckedChangeListener((buttonView, isChecked) ->
                onDirectionChange(!isChecked)
        );

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

        chb_group.setOnCheckedChangeListener((compoundButton, b) -> {
            if (updating) return;

            if (b) {
                if (!multitrack.contains(train))
                    multitrack.add(train);
            } else {
                multitrack.remove(train);
            }
        });

        chb_total.setOnCheckedChangeListener((compoundButton, b) -> {
            if (!updating) train.setTotal(b);
        });

        ib_release.setOnClickListener(this::ib_ReleaseClick);
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        confSpeedVolume = preferences.getBoolean("SpeedVolume", false);
        confAvailableFunctions = preferences.getBoolean("OnlyAvailableFunctions", true);
        // show DCC stop when DCC status is known and different from GO
        Boolean dccState = TCPClientApplication.getInstance().dccState;
        updateDccState(dccState == null || dccState);
    }

    private void fillHVs() {
        managed_str.clear();
        managed.clear();

        s_spinner.setEnabled(TrainDb.instance.trains.size() > 0);
        if (TrainDb.instance.trains.size() == 0)
            managed_str.add(getString(R.string.ta_no_loks));

        managed = new ArrayList<>(TrainDb.instance.trains.values());
        Collections.sort(managed, (train1, train2) -> train1.addr - train2.addr);
        int i = 0;
        for(Train t : managed) {
            managed_str.add(t.name + " (" + t.label + ") : " + t.addr);
            if (t == train) s_spinner.setSelection(i);
            i++;
        }

        // update multitraction
        for (i = multitrack.size()-1; i >= 0; i--)
            if (!managed.contains(multitrack.get(i)))
                multitrack.remove(i);

        if (train == null || !TrainDb.instance.trains.containsValue(train)) {
            if (!TrainDb.instance.trains.isEmpty())
                train = managed.get(0);
            else
                train = null;
        }

        managed_adapter.notifyDataSetChanged();
        this.updateGUTtoHV();
    }

    private void onDirectionChange(boolean newDir) {
        if (updating) return;

        if (!newDir)
            s_direction.setText(R.string.ta_direction_forward);
        else
            s_direction.setText(R.string.ta_direction_backwards);

        train.setDirection(newDir);

        if (multitrack.contains(train)) {
            for (Train s : multitrack) {
                if (s == train)
                    s.setDirection(newDir);
                else if (!s.stolen)
                    s.setDirection(!s.direction);
            }
        } else {
            train.setDirection(newDir);
        }
    }

    private void updateGUTtoHV() {
        this.updating = true;
        try {
            chb_group.setEnabled(train != null && managed.size() >= 2);
            chb_total.setEnabled(train != null && !train.stolen);
            lv_functions.setEnabled(train != null && !train.stolen);
            if (managed.size() < 2) chb_group.setChecked(false);

            if (train == null) {
                sb_speed.setProgress(0);
                s_direction.setChecked(false);
                s_direction.setText("-");

                chb_group.setChecked(false);
                tv_kmhSpeed.setText("- km/h");
                chb_total.setChecked(false);
                ib_release.setEnabled(false);

                tv_expSpeed.setText("- km/h");
                scom_expSignal.setCode(-1);
                tv_expSignalBlock.setText("");

                this.setEnabled(false);

                //set custom adapter with check boxes to list view
                FunctionCheckBoxAdapter dataAdapter = new FunctionCheckBoxAdapter(context,
                        R.layout.lok_function, new ArrayList<>(Arrays.asList(TrainFunction.DEF_FUNCTION)), false);
                lv_functions.setAdapter(dataAdapter);

                ib_status.setImageResource(R.drawable.ic_circle_gray);

            } else {
                sb_speed.setProgress(train.stepsSpeed);
                s_direction.setChecked(!train.direction);
                if (!train.direction)
                    s_direction.setText(R.string.ta_direction_forward);
                else
                    s_direction.setText(R.string.ta_direction_backwards);

                chb_group.setChecked(multitrack.contains(train));
                tv_kmhSpeed.setText(String.format("%s km/h", train.kmphSpeed));
                chb_total.setChecked(train.total);
                ib_release.setEnabled(true);

                if (train.expSpeed != -1)
                    tv_expSpeed.setText(String.format("%s km/h", train.expSpeed));
                else tv_expSpeed.setText("- km/h");

                if (train.expSignalCode != -1) {
                    scom_expSignal.setCode(train.expSignalCode);
                    tv_expSignalBlock.setText(train.expSignalBlock);
                }
                else {
                    scom_expSignal.setCode(-1);
                    tv_expSignalBlock.setText("");
                }

                this.setEnabled(train.total);

                //set custom adapter with check boxes to list view
                ArrayList<TrainFunction> functions;
                if (confAvailableFunctions) {
                    // just own filter
                    functions = new ArrayList<>();
                    for (int i = 0; i < train.function.length; i++)
                        if (!train.function[i].name.equals(""))
                            functions.add(train.function[i]);
                } else {
                    functions = new ArrayList<>(Arrays.asList(train.function));
                }
                FunctionCheckBoxAdapter dataAdapter = new FunctionCheckBoxAdapter(context,
                        R.layout.lok_function, functions, true);
                lv_functions.setAdapter(dataAdapter);

                if (train.stolen)
                    ib_status.setImageResource(R.drawable.ic_circle_yellow);
                else
                    ib_status.setImageResource(R.drawable.ic_circle_green);
            }

            ib_release.setAlpha(ib_release.isEnabled() ? 1f : 0.5f);
        }
        finally {
            this.updating = false;
        }
    }

    private void updateDccState(boolean enabled) {
        if (enabled) {
            ib_dcc.clearAnimation();
            ib_dcc.setAlpha(0.0f);
        }
        else {
            Animation blink = new AlphaAnimation(0.0f, 1.0f);
            blink.setDuration(250);
            blink.setRepeatMode(Animation.REVERSE);
            blink.setRepeatCount(Animation.INFINITE);
            ib_dcc.setAlpha(1.0f);
            ib_dcc.startAnimation(blink);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!confSpeedVolume || !sb_speed.isEnabled()) {
            return super.dispatchKeyEvent(event);
        }

        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN && sb_speed.isEnabled() && sb_speed.getProgress() < sb_speed.getMax()) {
                    sb_speed.setProgress(sb_speed.getProgress()+1);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN && sb_speed.isEnabled() && sb_speed.getProgress() > 0) {
                    sb_speed.setProgress(sb_speed.getProgress()-1);
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void onFuncChanged(int index, Boolean newState) {
        if (train != null && !train.stolen)
            train.setFunc(index, newState);
    }

    public void b_stopClick(View view) {
        sb_speed.setProgress(0);
        if (multitrack.contains(train)) {
            for (Train t : multitrack)
                t.emergencyStop();
        } else {
            train.emergencyStop();
        }
    }

    public void b_idleClick(View view) {
        sb_speed.setProgress(0);

        if (train == null) return;
        if (multitrack.contains(train)) {
            for (Train t : multitrack)
                if (!t.stolen)
                    t.setSpeedSteps(0);
        } else {
            if (train.total && !train.stolen)
                train.setSpeedSteps(0);
        }
    }

    public void ib_StatusClick(View v) {
        if (train != null && train.stolen)
            train.please();
    }

    public void ib_ReleaseClick(View v) {
        if (train == null) return;

        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.trl_really) + " " + train.name + "?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    train.release();
                    ib_release.setEnabled(false);
                    ib_release.setAlpha(ib_release.isEnabled() ? 1f : 0.5f);
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> {}).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokChangeEvent event) {
        if (train != null && event.getAddr() == train.addr)
            this.updateGUTtoHV();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokAddEvent event) {
        this.fillHVs();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokRemoveEvent event) {
        this.fillHVs();

        Toast.makeText(getApplicationContext(),
                R.string.trl_loko_released, Toast.LENGTH_LONG)
                .show();

        if (TrainDb.instance.trains.size() == 0)
            startActivity(new Intent(this, TrainRequest.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPDisconnectEvent event) {
        updating = true;
        managed_str.clear();
        managed.clear();

        s_spinner.setEnabled(false);
        managed_str.add(getString(R.string.ta_no_loks));

        train = null;
        this.updateGUTtoHV();

        updating = false;
        super.onEventMainThread(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokRespEvent event) {
        if (train == null) return;
        if (Integer.parseInt(event.getParsed().get(2)) != train.addr) return;

        tv_kmhSpeed.setText(String.format("%s km/h", train.kmphSpeed));

        if (event.getParsed().get(4).toUpperCase().equals("OK"))
            ib_status.setImageResource(R.drawable.ic_circle_green);
        else
            ib_status.setImageResource(R.drawable.ic_circle_red);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DccEvent event) {
        updateDccState(event.getParsed().get(2).toUpperCase().equals("GO"));
    }

    private void setEnabled(boolean enabled) {
        s_direction.setEnabled(enabled);
        sb_speed.setEnabled(enabled);
        b_stop.setEnabled(enabled);
        b_idle.setEnabled(enabled);
    }

    @Override
    protected void onPause() {
        b_idleClick(findViewById(R.id.startButton1));
        timerHandler.removeCallbacks(timerRunnable);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            b_idleClick(findViewById(R.id.startButton1));
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.fillHVs();
        timerHandler.postDelayed(timerRunnable, 100);
        if(!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        if(EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

}
