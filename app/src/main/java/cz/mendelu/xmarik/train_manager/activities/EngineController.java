package cz.mendelu.xmarik.train_manager.activities;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import cz.kudlav.scomview.ScomView;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.adapters.FunctionCheckBoxAdapter;
import cz.mendelu.xmarik.train_manager.events.LokChangeEvent;
import cz.mendelu.xmarik.train_manager.events.LokRemoveEvent;
import cz.mendelu.xmarik.train_manager.events.LokRespEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;
import cz.mendelu.xmarik.train_manager.models.Engine;
import cz.mendelu.xmarik.train_manager.models.EngineFunction;
import cz.mendelu.xmarik.train_manager.network.TCPClient;
import cz.mendelu.xmarik.train_manager.storage.TimeHolder;
import cz.mendelu.xmarik.train_manager.storage.EngineDb;


public class EngineController extends NavigationBase {
    private Engine engine;
    private boolean updating;
    private boolean error;
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
            if (!updating && engine != null && engine.total && !engine.stolen && engine.stepsSpeed != sb_speed.getProgress()) {
                if (engine.multitrack) {
                    for (Engine t : EngineDb.instance.engines.values())
                        if (t.multitrack && !t.stolen)
                            t.setSpeedSteps(sb_speed.getProgress());
                } else
                    engine.setSpeedSteps(sb_speed.getProgress());
            }
            timerHandler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_engine_controller);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.updating = false;

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

        functionAdapter = new FunctionCheckBoxAdapter(this, R.layout.lok_function);
        lv_functions.setAdapter(functionAdapter);

        // select train
        int train_addr;
        if (savedInstanceState != null)
            train_addr = savedInstanceState.getInt("train_addr", -1); // from saved state
        else
            train_addr = getIntent().getIntExtra("train_addr", -1); // from intent
        if (train_addr != -1)
            engine = EngineDb.instance.engines.get(train_addr);
        else
            engine = null;

        this.updateGUIFromTrain(); // will close activity in case train = null

        // Setup Time and DCC state observers
        observeTime();
        observeDccState();

        // GUI events:
        s_direction.setOnCheckedChangeListener((buttonView, checked) ->  onDirectionChange(!checked));
        chb_group.setOnCheckedChangeListener((compoundButton, checked) -> this.onChbGroupCheckedChange(compoundButton, checked));
        chb_total.setOnCheckedChangeListener((compoundButton, checked) -> this.onChbTotalCheckedChange(compoundButton, checked));

        ib_release.setOnClickListener(this::ib_ReleaseClick);
    }

    @Override
    public void onStart() {
        super.onStart();

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.setRequestedOrientation((preferences.getBoolean("FreezeScreenEngine", false)) ? SCREEN_ORIENTATION_LOCKED : SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // select train
        int train_addr = intent.getIntExtra("train_addr", -1);
        if (train_addr != -1) {
            engine = EngineDb.instance.engines.get(train_addr);
            this.updateGUIFromTrain();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("train_addr", engine.addr);
    }

    private void onChbTotalCheckedChange(CompoundButton buttonView, boolean checked) {
        if (this.updating)
            return;

        engine.setTotal(checked);
        if (!checked)
            engine.multitrack = false;
    }

    private void onChbGroupCheckedChange(CompoundButton buttonView, boolean checked) {
        if (this.updating)
            return;

        engine.multitrack = checked;
        if (checked) {
            displayGroupDialog();
        } else {
            for (Engine t : EngineDb.instance.engines.values()) {
                if (t.multitrack) {
                    displayUngroupDialog();
                    break;
                }
            }
        }
    }

    private void onDirectionChange(boolean newDir) {
        if (this.updating)
            return;

        if (!newDir)
            s_direction.setText(R.string.ta_direction_forward);
        else
            s_direction.setText(R.string.ta_direction_backwards);

        engine.setDirection(newDir);

        if (engine.multitrack) {
            for (Engine t : EngineDb.instance.engines.values()) {
                if (t.multitrack) {
                    if (t == engine)
                        t.setDirection(newDir);
                    else if (!t.stolen)
                        t.setDirection(!t.direction);
                }
            }
        } else {
            engine.setDirection(newDir);
        }
    }

    private void updateGUIFromTrain() {
        if ((engine == null) || (!EngineDb.instance.engines.containsValue(engine))) {
            startRequestActivity();
            return;
        }

        this.updating = true;
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        toolbar.setTitle(engine.getTitle());

        chb_total.setEnabled(!engine.stolen);
        lv_functions.setEnabled(engine != null && !engine.stolen);

        sb_speed.setProgress(engine.stepsSpeed);
        s_direction.setChecked(!engine.direction);
        if (!engine.direction)
            s_direction.setText(R.string.ta_direction_forward);
        else
            s_direction.setText(R.string.ta_direction_backwards);

        if (EngineDb.instance.engines.size() < 2) {
            engine.multitrack = false;
            chb_group.setEnabled(false);
        } else {
            chb_group.setEnabled(engine.total && !engine.stolen);
        }
        chb_group.setChecked(engine.multitrack);

        tv_kmhSpeed.setText(String.format("%s km/h", engine.kmphSpeed));
        chb_total.setChecked(engine.total);

        if (engine.expSpeed != -1)
            tv_expSpeed.setText(String.format("%s km/h", engine.expSpeed));
        else tv_expSpeed.setText("- km/h");

        scom_expSignal.setCode(engine.expSignalCode);
        tv_expSignalBlock.setText( (engine.expSignalCode != -1) ? engine.expSignalBlock : "" );

        this.setEnabled(engine.total);

        //set custom adapter with check boxes to list view
        ArrayList<EngineFunction> functions;
        if (preferences.getBoolean("OnlyAvailableFunctions", true)) {
            // just own filter
            functions = new ArrayList<>();
            for (int i = 0; i < engine.function.length; i++)
                if (!engine.function[i].name.equals(""))
                    functions.add(engine.function[i]);
        } else {
            functions = new ArrayList<>(Arrays.asList(engine.function));
        }
        functionAdapter.clear();
        functionAdapter.addAll(functions);

        this.updateStatus(false);

        this.updating = false;
    }

    private void observeTime() {
        TimeHolder.instance.used.observe(this, used -> {
            if (used) {
                TimeHolder.instance.time.observe(this, time -> tv_time.setText(time));
            } else {
                TimeHolder.instance.time.removeObservers(this);
                tv_time.setText("--:--:--");
            }
        });
        TimeHolder.instance.running.observe(this, running -> {
            tv_time.setTextColor((running) ? getResources().getColor(R.color.colorText) : getResources().getColor(R.color.colorDisabled));
        });
    }

    /**
     * Show DCC stop when DCC status is known and different from GO
     */
    private void observeDccState() {
        TCPClient.getInstance().dccState.observe(this, enabled -> {
            if (enabled != null && !enabled) {
                Animation blink = new AlphaAnimation(0.0f, 1.0f);
                blink.setDuration(250);
                blink.setRepeatMode(Animation.REVERSE);
                blink.setRepeatCount(Animation.INFINITE);
                ib_dcc.setAlpha(1.0f);
                ib_dcc.startAnimation(blink);
            } else {
                ib_dcc.clearAnimation();
                ib_dcc.setAlpha(0.0f);
            }
        });
    }

    private void updateStatus(boolean error) {
        this.error = error;
        if (engine.stolen) {
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
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean("SpeedVolume", false)) {
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
        if (engine != null && !engine.stolen)
            engine.setFunc(index, newState);
    }

    public void b_stopClick(View view) {
        sb_speed.setProgress(0);
        if (engine.multitrack) {
            for (Engine t : EngineDb.instance.engines.values())
                if (t.multitrack) t.emergencyStop();
        } else {
            engine.emergencyStop();
        }
    }

    public void b_idleClick(View view) {
        sb_speed.setProgress(0);

        if (engine == null) return;
        if (engine.multitrack) {
            for (Engine t : EngineDb.instance.engines.values())
                if (t.multitrack && !t.stolen) t.setSpeedSteps(0);
        } else {
            if (engine.total && !engine.stolen) engine.setSpeedSteps(0);
        }
    }

    public void ib_StatusClick(View v) {
        if (engine.stolen) {
            engine.please();
            Toast.makeText(this, R.string.ta_state_stolen, Toast.LENGTH_SHORT).show();
        } else if (this.error) {
            Toast.makeText(this, R.string.ta_state_err, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.ta_state_ok, Toast.LENGTH_SHORT).show();
        }
    }

    public void ib_ReleaseClick(View v) {
        if (engine == null) return;

        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.ta_release_really) + " " + engine.name + "?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> engine.release())
                .setNegativeButton(getString(R.string.no), (dialog, which) -> {}).show();
    }

    public void setTrain(Engine t) {
        engine = t;
        this.updateGUIFromTrain();
    }

    private void displayGroupDialog() {
        final ArrayList<Engine> engines = new ArrayList<>(EngineDb.instance.engines.values());
        engines.remove(this.engine);
        Collections.sort(engines, (engine1, engine2) -> engine1.addr - engine2.addr);
        final CharSequence[] trainsTitle = new CharSequence[engines.size()];
        final boolean[] trainsChecked = new boolean[engines.size()];
        int i = 0;
        for (Engine engine : engines) {
            trainsTitle[i] = engine.getTitle();
            trainsChecked[i] = engine.multitrack;
            i++;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.ta_dialog_group_title)
                .setMultiChoiceItems(trainsTitle, trainsChecked, (dialog, index, check) ->
                        trainsChecked[index] = check
                )
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    for (int j = 0; j < trainsChecked.length; j++) {
                        Engine engine = engines.get(j);
                        if (engine.multitrack != trainsChecked[j]) {
                            if (!engine.total) engine.setTotal(true);
                            engine.multitrack = !engine.multitrack;
                        }
                    }
                })
                .show();
    }

    private void displayUngroupDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.ta_dialog_ungroup_msg)
                .setPositiveButton(R.string.ta_dialog_ungroup_ok, (dialog, which) -> {
                    for (Engine t : EngineDb.instance.engines.values())
                        t.multitrack = false;
                })
                .setNegativeButton(R.string.ta_dialog_ungroup_cancel, null)
                .show();
    }

    private void startRequestActivity() {
        startActivity(new Intent(this, EngineRequest.class));
        this.finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokChangeEvent event) {
        super.onEventMainThread(event);
        if (engine != null && event.getAddr() == engine.addr)
            this.updateGUIFromTrain();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokRemoveEvent event) {
        super.onEventMainThread(event);

        Toast.makeText(getApplicationContext(),
                String.format(getString(R.string.ta_release_ok), event.getAddr()), Toast.LENGTH_LONG)
                .show();

        if (event.getAddr() == this.engine.addr) {
            this.finish();
            this.currentEngineClosed();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPDisconnectedEvent event) {
        this.startRequestActivity();
        super.onEventMainThread(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokRespEvent event) {
        if (engine == null) return;
        if (Integer.parseInt(event.getParsed().get(2)) != engine.addr) return;

        tv_kmhSpeed.setText(String.format("%s km/h", engine.kmphSpeed));

        this.updateStatus(!event.getParsed().get(4).equalsIgnoreCase("OK"));
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
        this.updateGUIFromTrain();
        timerHandler.postDelayed(timerRunnable, 100);
    }

}
