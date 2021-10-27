package cz.mendelu.xmarik.train_manager.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import cz.kudlav.scomview.ScomView;
import cz.mendelu.xmarik.train_manager.events.DccEvent;
import cz.mendelu.xmarik.train_manager.events.TimeEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectEvent;
import cz.mendelu.xmarik.train_manager.network.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.storage.TimeHolder;
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
    private Train train;
    private boolean updating;
    private boolean error;
    private Context context;
    private boolean confSpeedVolume;
    private boolean confAvailableFunctions;
    private Toolbar toolbar;
    private FunctionCheckBoxAdapter functionAdapter;


    private SeekBar sb_speed;
    private SwitchCompat s_direction;
    private CheckBox chb_total;
    private Button b_idle;
    private CheckBox chb_group;
    private ImageButton ib_status;
    private ImageButton ib_dcc;
    private ListView lv_functions;
    private TextView tv_kmhSpeed;
    private TextView tv_expSpeed;
    private TextView tv_expSignalBlock;
    private TextView tv_time;
    private ScomView scom_expSignal;
    private ImageButton ib_release;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!updating && train != null && train.total && !train.stolen && train.stepsSpeed != sb_speed.getProgress()) {
                if (train.multitrack) {
                    for (Train t : TrainDb.instance.trains.values())
                        if (t.multitrack && !t.stolen)
                            t.setSpeedSteps(sb_speed.getProgress());
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

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updating = false;
        context = this;

        sb_speed = findViewById(R.id.speedkBar1);
        s_direction = findViewById(R.id.handlerDirection1);
        b_idle = findViewById(R.id.startButton1);
        chb_group = findViewById(R.id.goupManaged1);
        ib_status = findViewById(R.id.ib_status);
        ib_dcc = findViewById(R.id.ib_dcc);
        ib_release = findViewById(R.id.ib_release);
        lv_functions = findViewById(R.id.checkBoxView1);
        tv_kmhSpeed = findViewById(R.id.kmh1);
        tv_expSpeed = findViewById(R.id.expSpeed);
        tv_expSignalBlock = findViewById(R.id.expSignalBlock);
        tv_time = findViewById(R.id.tvTime);
        scom_expSignal = findViewById(R.id.scom_view);
        chb_total = findViewById(R.id.totalManaged);

        functionAdapter = new FunctionCheckBoxAdapter(context, R.layout.lok_function);
        lv_functions.setAdapter(functionAdapter);

        // select train
        int train_addr;
        if (savedInstanceState != null)
            train_addr = savedInstanceState.getInt("train_addr", -1); // from saved state
        else
            train_addr = getIntent().getIntExtra("train_addr", -1); // from intent
        if (train_addr != -1)
            train = TrainDb.instance.trains.get(train_addr);

        this.fillHVs();

        // Time observers
        TimeHolder.instance.used.observe(this, used -> {
            if (used) {
                TimeHolder.instance.time.observe(this, time -> tv_time.setText(time));
            } else {
                TimeHolder.instance.time.removeObservers(this);
                tv_time.setText("");
            }
        });
        TimeHolder.instance.running.observe(this, running -> {
            if (running) {
                // color normal
                tv_time.setTextColor(getResources().getColor(R.color.colorText));
            } else {
                tv_time.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        });

        // GUI events:
        s_direction.setOnCheckedChangeListener((buttonView, checked) ->
                onDirectionChange(!checked)
        );

        chb_group.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (!updating) {
                train.multitrack = checked;
                if (checked) {
                    displayGroupDialog();
                } else {
                    for (Train t : TrainDb.instance.trains.values()) {
                        if (t.multitrack) {
                            displayUngroupDialog();
                            break;
                        }
                    }
                }
            }
        });

        chb_total.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (!updating) {
                train.setTotal(checked);
                if (!checked) train.multitrack = false;
            }
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // select train
        int train_addr = intent.getIntExtra("train_addr", -1);
        if (train_addr != -1) {
            train = TrainDb.instance.trains.get(train_addr);
            this.fillHVs();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("train_addr", train.addr);
    }

    private void fillHVs() {
        if (TrainDb.instance.trains.isEmpty()) {
            startRequestActivity();
            return;
        }

        if (train == null || !TrainDb.instance.trains.containsValue(train)) {
            int min_addr = Integer.MAX_VALUE;
            for (Train t: TrainDb.instance.trains.values())
                if (min_addr > t.addr) min_addr = t.addr;
            train = TrainDb.instance.trains.get(min_addr);
        }

        toolbar.setTitle(train.getTitle());

        this.updateGUTtoHV();
    }

    private void onDirectionChange(boolean newDir) {
        if (updating) return;

        if (!newDir)
            s_direction.setText(R.string.ta_direction_forward);
        else
            s_direction.setText(R.string.ta_direction_backwards);

        train.setDirection(newDir);

        if (train.multitrack) {
            for (Train t : TrainDb.instance.trains.values()) {
                if (t.multitrack) {
                    if (t == train)
                        t.setDirection(newDir);
                    else if (!t.stolen)
                        t.setDirection(!t.direction);
                }
            }
        } else {
            train.setDirection(newDir);
        }
    }

    private void updateGUTtoHV() {
        if (train == null) {
            startRequestActivity();
            return;
        }

        this.updating = true;

        chb_total.setEnabled(!train.stolen);
        lv_functions.setEnabled(train != null && !train.stolen);

        sb_speed.setProgress(train.stepsSpeed);
        s_direction.setChecked(!train.direction);
        if (!train.direction)
            s_direction.setText(R.string.ta_direction_forward);
        else
            s_direction.setText(R.string.ta_direction_backwards);

        if (TrainDb.instance.trains.size() < 2) {
            train.multitrack = false;
            chb_group.setEnabled(false);
        } else {
            chb_group.setEnabled(train.total && !train.stolen);
        }
        chb_group.setChecked(train.multitrack);

        tv_kmhSpeed.setText(String.format("%s km/h", train.kmphSpeed));
        chb_total.setChecked(train.total);

        if (train.expSpeed != -1)
            tv_expSpeed.setText(String.format("%s km/h", train.expSpeed));
        else tv_expSpeed.setText("- km/h");

        scom_expSignal.setCode(train.expSignalCode);
        tv_expSignalBlock.setText( (train.expSignalCode != -1) ? train.expSignalBlock : "" );

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
        functionAdapter.clear();
        functionAdapter.addAll(functions);

        this.updateStatus(false);

        this.updating = false;
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

    private void updateStatus(boolean error) {
        this.error = error;
        if (train.stolen) {
            ib_status.setImageResource(R.drawable.ic_circle_yellow);
        } else if (error) {
            ib_status.setImageResource(R.drawable.ic_circle_red);
            Toast.makeText(this, R.string.ta_state_err, Toast.LENGTH_SHORT).show();
        } else {
            ib_status.setImageResource(R.drawable.ic_circle_green);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!confSpeedVolume) {
            return super.dispatchKeyEvent(event);
        }

        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (sb_speed.isEnabled() && action == KeyEvent.ACTION_DOWN && sb_speed.getProgress() < sb_speed.getMax()) {
                    sb_speed.setProgress(sb_speed.getProgress()+1);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (sb_speed.isEnabled() && action == KeyEvent.ACTION_DOWN && sb_speed.getProgress() > 0) {
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
        if (train.multitrack) {
            for (Train t : TrainDb.instance.trains.values())
                if (t.multitrack) t.emergencyStop();
        } else {
            train.emergencyStop();
        }
    }

    public void b_idleClick(View view) {
        sb_speed.setProgress(0);

        if (train == null) return;
        if (train.multitrack) {
            for (Train t : TrainDb.instance.trains.values())
                if (t.multitrack && !t.stolen) t.setSpeedSteps(0);
        } else {
            if (train.total && !train.stolen) train.setSpeedSteps(0);
        }
    }

    public void ib_StatusClick(View v) {
        if (train.stolen) {
            train.please();
            Toast.makeText(this, R.string.ta_state_stolen, Toast.LENGTH_SHORT).show();
        } else if (this.error) {
            Toast.makeText(this, R.string.ta_state_err, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.ta_state_ok, Toast.LENGTH_SHORT).show();
        }
    }

    public void ib_ReleaseClick(View v) {
        if (train == null) return;

        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.ta_release_really) + " " + train.name + "?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> train.release())
                .setNegativeButton(getString(R.string.no), (dialog, which) -> {}).show();
    }

    public void setTrain(Train t) {
        train = t;
        this.fillHVs();
    }

    private void displayGroupDialog() {
        final ArrayList<Train> trains = new ArrayList<>(TrainDb.instance.trains.values());
        trains.remove(this.train);
        Collections.sort(trains, (train1, train2) -> train1.addr - train2.addr);
        final CharSequence[] trainsTitle = new CharSequence[trains.size()];
        final boolean[] trainsChecked = new boolean[trains.size()];
        int i = 0;
		for (Train train: trains) {
            trainsTitle[i] = train.getTitle();
            trainsChecked[i] = train.multitrack;
            i++;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.ta_dialog_group_title)
                .setMultiChoiceItems(trainsTitle, trainsChecked, (dialog, index, check) ->
                        trainsChecked[index] = check
                )
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    for (int j = 0; j < trainsChecked.length; j++) {
                        Train train = trains.get(j);
                        if (train.multitrack != trainsChecked[j]) {
                            if (!train.total) train.setTotal(true);
                            train.multitrack = !train.multitrack;
                        }
                    }
                })
                .show();
    }

    private void displayUngroupDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.ta_dialog_ungroup_msg)
                .setPositiveButton(R.string.ta_dialog_ungroup_ok, (dialog, which) -> {
                    for (Train t : TrainDb.instance.trains.values())
                        t.multitrack = false;
                })
                .setNegativeButton(R.string.ta_dialog_ungroup_cancel, null)
                .show();
    }

    private void startRequestActivity() {
        startActivity(new Intent(this, TrainRequest.class));
        this.finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokChangeEvent event) {
        super.onEventMainThread(event);
        if (train != null && event.getAddr() == train.addr)
            this.updateGUTtoHV();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokAddEvent event) {
        super.onEventMainThread(event);
        this.fillHVs();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokRemoveEvent event) {
        super.onEventMainThread(event);
        this.fillHVs();

        Toast.makeText(getApplicationContext(),
                R.string.ta_release_ok, Toast.LENGTH_LONG)
                .show();

        if (TrainDb.instance.trains.size() == 0)
            startActivity(new Intent(this, TrainRequest.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPDisconnectEvent event) {
        updating = true;

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

        this.updateStatus(!event.getParsed().get(4).equalsIgnoreCase("OK"));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DccEvent event) {
        updateDccState(event.getParsed().get(2).equalsIgnoreCase("GO"));
    }

    private void setEnabled(boolean enabled) {
        s_direction.setEnabled(enabled);
        sb_speed.setEnabled(enabled);
        b_idle.setEnabled(enabled);
    }

    @Override
    public void onPause() {
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
