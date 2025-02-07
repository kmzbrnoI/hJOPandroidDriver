package cz.mendelu.xmarik.train_manager.activities;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

import static cz.mendelu.xmarik.train_manager.models.Engine.EXP_SPEED_UNKNOWN;
import static cz.mendelu.xmarik.train_manager.models.Engine.SIGNAL_UNKNOWN;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import android.widget.RadioGroup;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import cz.kudlav.scomview.ScomView;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.adapters.FunctionCheckBoxAdapter;
import cz.mendelu.xmarik.train_manager.events.EngineChangeEvent;
import cz.mendelu.xmarik.train_manager.events.EngineRemoveEvent;
import cz.mendelu.xmarik.train_manager.events.EngineRespEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;
import cz.mendelu.xmarik.train_manager.models.Engine;
import cz.mendelu.xmarik.train_manager.models.EngineFunction;
import cz.mendelu.xmarik.train_manager.network.TCPClient;
import cz.mendelu.xmarik.train_manager.storage.TimeHolder;
import cz.mendelu.xmarik.train_manager.storage.EngineDb;


public class EngineController extends NavigationBase {
    private static final int TIMER_SET_SPEED_INTERVAL_MS = 100;

    private Engine engine;
    private boolean updating;
    private boolean error;
    private Toolbar toolbar;
    private FunctionCheckBoxAdapter functionAdapter;

    ATP atp;

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
    private RadioGroup rg_atp_mode;

    Handler t_setSpeedHandler = new Handler();
    Runnable t_setSpeedRunnable = new Runnable() {
        @Override
        public void run() { timerSetSpeedRun(); }
    };

    void timerSetSpeedRun() {
        if ((!this.updating) && (this.engine != null) && (this.engine.isMyControl()) && (this.engine.stepsSpeed != this.sb_speed.getProgress())) {
            this.engine.setSpeedSteps(sb_speed.getProgress());

            if (this.engine.multitrack)
                for (Engine t : EngineDb.instance.engines.values())
                    if ((t != this.engine) && (t.multitrack && t.isMyControl()))
                        t.setSpeedSteps(sb_speed.getProgress());
        }
        t_setSpeedHandler.postDelayed(t_setSpeedRunnable, TIMER_SET_SPEED_INTERVAL_MS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_engine_controller);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.atp = new ATP();

        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);

        this.updating = false;

        this.sb_speed = findViewById(R.id.speedkBar1);
        this.s_direction = findViewById(R.id.handlerDirection1);
        this.b_idle = findViewById(R.id.startButton1);
        this.chb_group = findViewById(R.id.goupManaged1);
        this.ib_status = findViewById(R.id.ib_status);
        this.ib_dcc = findViewById(R.id.ib_dcc);
        this.ib_release = findViewById(R.id.ib_release);
        this.lv_functions = findViewById(R.id.checkBoxView1);
        this.tv_kmhSpeed = findViewById(R.id.kmh1);
        this.tv_expSpeed = findViewById(R.id.expSpeed);
        this.tv_expSignalBlock = findViewById(R.id.expSignalBlock);
        this.tv_time = findViewById(R.id.tvTime);
        this.scom_expSignal = findViewById(R.id.scom_view);
        this.chb_total = findViewById(R.id.totalManaged);
        this.rg_atp_mode = findViewById(R.id.rgATPMode);

        this.functionAdapter = new FunctionCheckBoxAdapter(this, R.layout.lok_function);
        this.lv_functions.setAdapter(this.functionAdapter);

        // select train
        int train_addr;
        Engine e = null;
        if (savedInstanceState != null)
            train_addr = savedInstanceState.getInt("train_addr", -1); // from saved state
        else
            train_addr = getIntent().getIntExtra("train_addr", -1); // from intent
        if (train_addr != -1)
            e = EngineDb.instance.engines.get(train_addr);

        this.setEngine(e); // will close activity in case train = null

        // Setup Time and DCC state observers
        this.observeTime();
        this.observeDccState();

        // GUI events:
        this.s_direction.setOnCheckedChangeListener((buttonView, checked) -> onDirectionChange(checked ? Engine.Direction.FORWARD : Engine.Direction.BACKWARD));
        this.chb_group.setOnCheckedChangeListener((compoundButton, checked) -> this.onChbGroupCheckedChange(compoundButton, checked));
        this.chb_total.setOnCheckedChangeListener((compoundButton, checked) -> this.onChbTotalCheckedChange(compoundButton, checked));
        this.rg_atp_mode.setOnCheckedChangeListener((group, checkedId) -> this.onRgATPModeCheckedChange(group, checkedId));

        this.ib_release.setOnClickListener(this::ib_ReleaseClick);
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
        final int train_addr = intent.getIntExtra("train_addr", -1);
        if (train_addr != -1)
            this.setEngine(EngineDb.instance.engines.get(train_addr));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("train_addr", this.engine.addr);
    }

    private void onChbTotalCheckedChange(CompoundButton buttonView, boolean checked) {
        if (this.updating)
            return;

        this.engine.setTotal(checked);
        if (!checked) {
            this.engine.multitrack = false;
            this.atp.mode = ATP.Mode.TRAIN;
        }
    }

    private void onRgATPModeCheckedChange(RadioGroup group, int checkedId) {
        if (this.updating)
            return;
        this.atp.mode = (checkedId == R.id.rATPtrain) ? ATP.Mode.TRAIN : ATP.Mode.SHUNT;
        this.updateGUIFromTrain();
    }

    private void onChbGroupCheckedChange(CompoundButton buttonView, boolean checked) {
        if (this.updating)
            return;

        this.engine.multitrack = checked;
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

    private void onDirectionChange(Engine.Direction newDir) {
        if (this.updating)
            return;

        this.s_direction.setText((newDir == Engine.Direction.FORWARD) ? R.string.ta_direction_forward : R.string.ta_direction_backwards);
        this.engine.setDirection(newDir);

        if (this.engine.multitrack)
            for (Engine t : EngineDb.instance.engines.values())
                if ((t != this.engine) && (t.multitrack) && (t.isMyControl()))
                    t.setDirection(Engine.invertDirection(t.direction));
    }

    public void setEngine(Engine e) {
        if (this.engine == e)
            return;

        this.engine = e;
        this.atp.mode = ATP.Mode.TRAIN;
        this.updateGUIFromTrain();
    }

    private void updateGUIFromTrain() {
        if ((engine == null) || (!EngineDb.instance.engines.containsValue(engine))) {
            this.gotoEngineRequestActivity();
            return;
        }

        this.updating = true;
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        this.toolbar.setTitle(this.engine.getTitle());

        this.chb_total.setEnabled(!this.engine.stolen);
        this.lv_functions.setEnabled(engine != null && !this.engine.stolen);

        this.sb_speed.setProgress(this.engine.stepsSpeed);
        this.s_direction.setChecked(this.engine.direction == Engine.Direction.FORWARD);
        this.s_direction.setText(this.engine.direction == Engine.Direction.FORWARD ? R.string.ta_direction_forward : R.string.ta_direction_backwards);

        if (EngineDb.instance.engines.size() < 2) {
            this.engine.multitrack = false;
            this.chb_group.setEnabled(false);
        } else {
            this.chb_group.setEnabled(this.engine.isMyControl());
        }
        this.chb_group.setChecked(this.engine.multitrack);

        this.tv_kmhSpeed.setText(String.format("%s km/h", this.engine.kmphSpeed));
        this.chb_total.setChecked(this.engine.total);

        if (this.atp.mode == ATP.Mode.TRAIN) {
            this.tv_expSpeed.setText((this.engine.expSpeed != EXP_SPEED_UNKNOWN) ? String.format("%s km/h", this.engine.expSpeed) : "- km/h");
            this.scom_expSignal.setCode(this.engine.expSignalCode);
            this.tv_expSignalBlock.setText((this.engine.expSignalCode != SIGNAL_UNKNOWN) ? this.engine.expSignalBlock : "---");
        } else { // SHUNT
            this.tv_expSpeed.setText(Integer.toString(ATP.SHUNT_MAX_SPEED_KMPH) + " km/h");
            this.scom_expSignal.setCode(SIGNAL_UNKNOWN);
            this.tv_expSignalBlock.setText("---");
        }

        this.rg_atp_mode.check((this.atp.mode == ATP.Mode.TRAIN) ? R.id.rATPtrain: R.id.rATPshunt);
        for (int i = 0; i < this.rg_atp_mode.getChildCount(); i++)
            this.rg_atp_mode.getChildAt(i).setEnabled(this.engine.isMyControl());

        this.setEnabled(this.engine.total);

        //set custom adapter with check boxes to list view
        ArrayList<EngineFunction> functions;
        if (preferences.getBoolean("OnlyAvailableFunctions", true)) {
            // just own filter
            functions = new ArrayList<>();
            for (int i = 0; i < this.engine.function.length; i++)
                if (!this.engine.function[i].name.equals(""))
                    functions.add(this.engine.function[i]);
        } else {
            functions = new ArrayList<>(Arrays.asList(this.engine.function));
        }
        this.functionAdapter.clear();
        this.functionAdapter.addAll(functions);

        this.updateStatus(false);
        this.atp.update();

        this.updating = false;
    }

    private void observeTime() {
        TimeHolder.instance.used.observe(this, used -> {
            if (used) {
                TimeHolder.instance.time.observe(this, time -> tv_time.setText(time));
            } else {
                TimeHolder.instance.time.removeObservers(this);
                this.tv_time.setText("--:--:--");
            }
        });
        TimeHolder.instance.running.observe(this, running -> {
            this.tv_time.setTextColor((running) ? getResources().getColor(R.color.colorText) : getResources().getColor(R.color.colorDisabled));
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
                this.ib_dcc.setAlpha(1.0f);
                this.ib_dcc.startAnimation(blink);
            } else {
                this.ib_dcc.clearAnimation();
                this.ib_dcc.setAlpha(0.0f);
            }
        });
    }

    private void updateStatus(boolean error) {
        this.error = error;
        if (this.engine.stolen) {
            this.ib_status.setImageResource(R.drawable.ic_circle_yellow);
        } else if (error) {
            this.ib_status.setImageResource(R.drawable.ic_circle_red);
            Toast.makeText(this, R.string.ta_state_err, Toast.LENGTH_SHORT).show();
        } else {
            this.ib_status.setImageResource(R.drawable.ic_circle_green);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean("SpeedVolume", false)) {
            return super.dispatchKeyEvent(event);
        }

        final int action = event.getAction();
        final int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if ((this.sb_speed.isEnabled()) && (action == KeyEvent.ACTION_DOWN) && (this.sb_speed.getProgress() < this.sb_speed.getMax())) {
                    this.sb_speed.incrementProgressBy(1);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if ((this.sb_speed.isEnabled()) && (action == KeyEvent.ACTION_DOWN) && (this.sb_speed.getProgress() > 0)) {
                    this.sb_speed.incrementProgressBy(-1);
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void onFuncChanged(int index, Boolean newState) {
        if (this.engine != null && !this.engine.stolen)
            this.engine.setFunc(index, newState);
    }

    public void b_stopClick(View view) {
        this.emergencyStop();
    }

    public void emergencyStop() {
        // Intentionally omit 'total' check - allow emergency stop even if not in total control
        this.sb_speed.setProgress(0);
        this.engine.emergencyStop();

        if (this.engine.multitrack)
            for (Engine t : EngineDb.instance.engines.values())
                if ((t != this.engine) && (t.multitrack))
                    t.emergencyStop();
    }

    public void b_idleClick(View view) {
        this.sb_speed.setProgress(0);

        if (engine == null)
            return;
        if (this.engine.multitrack) {
            for (Engine t : EngineDb.instance.engines.values())
                if (t.multitrack && t.isMyControl())
                    t.setSpeedSteps(0);
        } else {
            if (this.engine.isMyControl())
                this.engine.setSpeedSteps(0);
        }
    }

    public void ib_StatusClick(View v) {
        if (this.engine.stolen) {
            this.engine.please();
            Toast.makeText(this, R.string.ta_state_stolen, Toast.LENGTH_SHORT).show();
        } else if (this.error) {
            Toast.makeText(this, R.string.ta_state_err, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.ta_state_ok, Toast.LENGTH_SHORT).show();
        }
    }

    public void ib_ReleaseClick(View v) {
        if (engine == null)
            return;

        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.ta_release_really) + " " + this.engine.getTitle() + "?")
            .setPositiveButton(getString(R.string.yes), (dialog, which) -> this.engine.release())
            .setNegativeButton(getString(R.string.no), (dialog, which) -> {}).show();
    }

    private void displayGroupDialog() {
        final ArrayList<Engine> engines = new ArrayList<>(EngineDb.instance.engines.values());
        engines.remove(this.engine);
        Collections.sort(engines, (engine1, engine2) -> engine1.addr - engine2.addr);
        final CharSequence[] enginesTitle = new CharSequence[engines.size()];
        final boolean[] enginesChecked = new boolean[engines.size()];
        int i = 0;
        for (Engine _engine : engines) {
            enginesTitle[i] = _engine.getTitle();
            enginesChecked[i] = _engine.multitrack;
            i++;
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.ta_dialog_group_title)
            .setMultiChoiceItems(enginesTitle, enginesChecked, (dialog, index, check) ->
                enginesChecked[index] = check
            )
            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                for (int j = 0; j < enginesChecked.length; j++) {
                    Engine _engine = engines.get(j);
                    if (_engine.multitrack != enginesChecked[j]) {
                        if (!_engine.total)
                            _engine.setTotal(true);
                        _engine.multitrack = enginesChecked[j];
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

    private void gotoEngineRequestActivity() {
        this.startActivity(new Intent(this, EngineRequest.class));
        this.finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EngineChangeEvent event) {
        super.onEventMainThread(event);
        if ((engine != null) && (event.getAddr() == this.engine.addr))
            this.updateGUIFromTrain();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EngineRemoveEvent event) {
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
        this.gotoEngineRequestActivity();
        super.onEventMainThread(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EngineRespEvent event) {
        if (engine == null)
            return;
        if (Integer.parseInt(event.getParsed().get(2)) != this.engine.addr)
            return;

        this.tv_kmhSpeed.setText(String.format("%s km/h", this.engine.kmphSpeed));
        this.updateStatus(!event.getParsed().get(4).equalsIgnoreCase("OK"));
        this.atp.update();
    }

    private void setEnabled(boolean enabled) {
        this.s_direction.setEnabled(enabled);
        this.sb_speed.setEnabled(enabled);
        this.b_idle.setEnabled(enabled);
    }

    @Override
    public void onPause() {
        this.b_idleClick(findViewById(R.id.startButton1));
        this.t_setSpeedHandler.removeCallbacks(t_setSpeedRunnable);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
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
        this.t_setSpeedHandler.postDelayed(t_setSpeedRunnable, TIMER_SET_SPEED_INTERVAL_MS);
    }

    // Automatic Train Protection / Vlakovy zabezpecovac
    class ATP {
        static final int SHUNT_MAX_SPEED_KMPH = 40;
        static final int OVERSPEED_DELAY_EB_MS = 3000;
        public enum Mode {
            TRAIN,
            SHUNT,
        }

        public Mode mode = Mode.TRAIN;
        private boolean overspeed;
        private MediaPlayer soundPlayer = new MediaPlayer();

        Handler t_overSpeedEB = new Handler(); // EB = emergency braking
        Runnable t_verSpeedEBRunnable = new Runnable() {
            @Override
            public void run() { overSpeedEB(); }
        };

        public ATP() {
            try {
                final Context appContent = EngineController.this.getApplicationContext();
                final AssetFileDescriptor afd = appContent.getResources().openRawResourceFd(R.raw.s2_warning);
                soundPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                soundPlayer.setLooping(true);
                soundPlayer.prepareAsync();
            } catch (IOException e) {
                Log.e("ATP ctor", "Sound load", e);
            }
        }

        public void update() {
            this.overSpeedUpdate();
        }

        private void overSpeedUpdate() {
            final Engine thisEngine = EngineController.this.engine;

            boolean overSpeed = false;
            if (thisEngine.isMyControl()) {
                if (this.mode == Mode.SHUNT) {
                    overSpeed = (thisEngine.kmphSpeed > SHUNT_MAX_SPEED_KMPH);
                } else { // mode == Mode.TRAIN
                    overSpeed = (thisEngine.expSpeed != EXP_SPEED_UNKNOWN) ? (thisEngine.kmphSpeed > thisEngine.expSpeed) : false;
                }
            }
            this.overSpeedSet(overSpeed);
        }

        private void overSpeedSet(boolean value) {
            if (value == this.overspeed)
                return;
            this.overspeed = value;

            if (value)
                this.overSpeedBegin();
            else
                this.overSpeedEnd();
        }

        private void overSpeedBegin() {
            this.t_overSpeedEB.postDelayed(t_verSpeedEBRunnable, OVERSPEED_DELAY_EB_MS);

            { // Animation
                Animation blink = new AlphaAnimation(0.0f, 1.0f);
                blink.setDuration(100);
                blink.setRepeatMode(Animation.REVERSE);
                blink.setRepeatCount(Animation.INFINITE);
                EngineController.this.tv_kmhSpeed.startAnimation(blink);
                EngineController.this.tv_expSpeed.startAnimation(blink);
            }

            { // Sound
                if (!this.soundPlayer.isPlaying())
                    this.soundPlayer.start();
            }
        }

        private void overSpeedEnd() {
            this.t_overSpeedEB.removeCallbacks(t_verSpeedEBRunnable);

            EngineController.this.tv_kmhSpeed.clearAnimation();
            EngineController.this.tv_expSpeed.clearAnimation();

            if (this.soundPlayer.isPlaying()) {
                this.soundPlayer.stop();
                this.soundPlayer.prepareAsync();
            }
        }

        private void overSpeedEB() {
            if (EngineController.this.engine.isMyControl()) {
                EngineController.this.emergencyStop();

                new AlertDialog.Builder(EngineController.this)
                    .setMessage(getString(R.string.ta_overspeed_eb_msg))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {})
                    .show();
            }
        }
    }
}
