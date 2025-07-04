package cz.mendelu.xmarik.train_manager.activities;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

import static cz.mendelu.xmarik.train_manager.models.Engine.EXP_SPEED_UNKNOWN;
import static cz.mendelu.xmarik.train_manager.models.Engine.SIGNAL_UNKNOWN;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import java.util.Timer;
import java.util.TimerTask;

import cz.kudlav.scomview.ScomView;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.adapters.FunctionCheckBoxAdapter;
import cz.mendelu.xmarik.train_manager.events.EngineAddEvent;
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
    private ImageButton ib_dcc_go;
    private ImageButton ib_dcc_stop;
    private ListView lv_functions;
    private TextView tv_kmhSpeed;
    private TextView tv_expSpeed;
    private TextView tv_expDirection;
    private TextView tv_expSignalBlock;
    private TextView tv_time;
    private ScomView scom_expSignal;
    /** @noinspection FieldCanBeLocal*/
    private ImageButton ib_release;
    private RadioGroup rg_atp_mode;

    private int lastExpSpeed = EXP_SPEED_UNKNOWN;
    private int lastExpSignalCode = SIGNAL_UNKNOWN;

    private MediaPlayer infoPlayer = null;
    private Vibrator vibrator;
    Handler t_setSpeedHandler = new Handler();
    Runnable t_setSpeedRunnable = new Runnable() {
        @Override
        public void run() { timerSetSpeedRun(); }
    };
    private boolean notifyTimerScheduled = false;

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

        try {
            this.infoPlayer = new MediaPlayer();
            final Context appContent = EngineController.this.getApplicationContext();
            final AssetFileDescriptor afd = appContent.getResources().openRawResourceFd(R.raw.s_info);
            this.infoPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            this.infoPlayer.prepareAsync();
        } catch (IOException e) {
            this.infoPlayer = null;
            Log.e("EC::onCreate", "s_info sound load", e);
        }

        this.vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        // ATP must be created after infoPlayer, it potentially needs it
        this.atp = new ATP();

        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        this.toolbar.setOnClickListener((v) -> toolbarOnClick(v));

        this.updating = false;

        this.sb_speed = findViewById(R.id.speedkBar1);
        this.s_direction = findViewById(R.id.s_direction);
        this.b_idle = findViewById(R.id.startButton1);
        this.chb_group = findViewById(R.id.goupManaged1);
        this.ib_status = findViewById(R.id.ib_status);
        this.ib_dcc_go = findViewById(R.id.ib_dcc_go);
        this.ib_dcc_stop = findViewById(R.id.ib_dcc_stop);
        this.ib_release = findViewById(R.id.ib_release);
        this.lv_functions = findViewById(R.id.checkBoxView1);
        this.tv_kmhSpeed = findViewById(R.id.kmh1);
        this.tv_expSpeed = findViewById(R.id.expSpeed);
        this.tv_expDirection = findViewById(R.id.expDirection);
        this.tv_expSignalBlock = findViewById(R.id.expSignalBlock);
        this.tv_time = findViewById(R.id.tvTime);
        this.scom_expSignal = findViewById(R.id.scom_view);
        this.chb_total = findViewById(R.id.totalManaged);
        this.rg_atp_mode = findViewById(R.id.rgATPMode);

        this.functionAdapter = new FunctionCheckBoxAdapter(this, R.layout.lok_function);
        this.lv_functions.setAdapter(this.functionAdapter);

        // select train
        {
            int train_addr;
            Engine e = null;
            if (savedInstanceState != null)
                train_addr = savedInstanceState.getInt("train_addr", -1); // from saved state
            else
                train_addr = getIntent().getIntExtra("train_addr", -1); // from intent
            if (train_addr != -1)
                e = EngineDb.instance.engines.get(train_addr);

            this.setEngine(e); // will close activity in case train = null
        }

        // Setup Time and DCC state observers
        this.observeTime();
        this.observeDccState();

        // GUI events:
        this.sb_speed.setOnSeekBarChangeListener(new SbSpeedChangeListener());
        this.s_direction.setOnCheckedChangeListener((buttonView, checked) -> onDirectionChange(checked ? Engine.Direction.FORWARD : Engine.Direction.BACKWARD));
        this.chb_group.setOnCheckedChangeListener((compoundButton, checked) -> this.onChbGroupCheckedChange(compoundButton, checked));
        this.chb_total.setOnCheckedChangeListener((compoundButton, checked) -> this.onChbTotalCheckedChange(compoundButton, checked));
        this.rg_atp_mode.setOnCheckedChangeListener((group, checkedId) -> this.onRgATPModeCheckedChange(group, checkedId));

        this.ib_release.setOnClickListener(this::ib_ReleaseClick);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.atp.onDestroy();
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

    private void toolbarOnClick(View v) {
        if (this.engine == null)
            return;

        final String message = "" +
            getString(R.string.engine_name) + ": " + this.engine.name + "\n" +
            getString(R.string.engine_owner) + ": " + this.engine.owner + "\n" +
            getString(R.string.engine_designation) + ": " + this.engine.designation + "\n" +
            getString(R.string.engine_address) + ": " + Integer.toString(this.engine.addr) + "\n" +
            getString(R.string.engine_type) + ": " + this.engine.kindStr(this) + "\n" +
            getString(R.string.engine_Vmax) + ": " + Integer.toString(this.engine.vmax) + " km/h\n" +
            getString(R.string.engine_note) + ": " + (!this.engine.note.isEmpty() ? this.engine.note : "none");

        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {})
                .show();
    }

    private void onChbTotalCheckedChange(CompoundButton buttonView, boolean checked) {
        if (this.updating)
            return;

        this.engine.setTotal(checked);
        if (checked) {
            if (EngineDb.instance.engines.size() >= 2)
                this.displayGroupDialog(getString(R.string.ta_dialog_group_title));
        } else {
            final boolean wasMultitrack = this.engine.multitrack;
            this.engine.multitrack = false;
            this.atp.mode = ATP.Mode.TRAIN;

            if ((wasMultitrack) && (EngineDb.instance.isAnyEngineMultitrack())) // actually isAnyOtherEngineMultitrack
                this.displayUntotalDialog();
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
            this.displayGroupDialog(getString(R.string.ta_dialog_group_title));
        } else {
            if (EngineDb.instance.isAnyEngineMultitrack()) // actually isAnyOtherEngineMultitrack
                this.displayUngroupDialog();
        }
    }

    class SbSpeedChangeListener implements SeekBar.OnSeekBarChangeListener {
        // onProgressChanged is called several times when user holds the finger on the seekbar
        // -> show dialog only once
        AlertDialog dialog;
        public SbSpeedChangeListener() {
            this.dialog = new AlertDialog.Builder(EngineController.this)
                    .setMessage(getString(R.string.ta_atp_movement_not_allowed))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {})
                    .create();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (EngineController.this.updating)
                return;

            if ((EngineController.this.engine.stepsSpeed == 0) && (progress > 0) && (!EngineController.this.atp.isMovementStartAllowed())) {
                EngineController.this.updating = true;
                EngineController.this.sb_speed.setProgress(0);
                EngineController.this.updating = false;

                if (!this.dialog.isShowing())
                    this.dialog.show();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }

    private void onDirectionChange(Engine.Direction newDir) {
        if (this.updating)
            return;

        if ((this.atp.mode == ATP.Mode.TRAIN) && (!this.engine.expDirectionMatch(newDir))) {
            this.s_direction.setChecked(this.engine.direction == Engine.Direction.FORWARD);
            new AlertDialog.Builder(this)
                    .setMessage(R.string.ta_atp_direction_change_not_allowed_msg)
                    .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {})
                    .show();
            return;
        }

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
        this.lastExpSpeed = this.engine.expSpeed;
        this.lastExpSignalCode = this.engine.expSignalCode;
        this.updateGUIFromTrain();
    }

    @SuppressLint("SetTextI18n")
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
            if ((this.engine.isMyControl()) && (!this.notifyTimerScheduled) &&
                    ((this.engine.expSpeed != this.lastExpSpeed) || (this.engine.expSignalCode != this.lastExpSignalCode))) {
                // Run notifyUser after 100 ms to leave time for ATP to start sound/vibration first
                this.notifyTimerScheduled = true;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        notifyTimerScheduled = false;
                        if (!atp.isWarning())
                            notifyUser();
                    }
                }, 100);
            }

            this.tv_expSpeed.setText((this.engine.expSpeed != EXP_SPEED_UNKNOWN) ? String.format("%s km/h", this.engine.expSpeed) : "- km/h");
            this.tv_expDirection.setText(this.engine.expDirectionStr(this));
            this.scom_expSignal.setCode(this.engine.expSignalCode);
            this.tv_expSignalBlock.setText((this.engine.expSignalCode != SIGNAL_UNKNOWN) ? this.engine.expSignalBlock : "---");
        } else { // SHUNT
            this.tv_expSpeed.setText(Integer.toString(ATP.SHUNT_MAX_SPEED_KMPH) + " km/h");
            this.tv_expDirection.setText("---");
            this.scom_expSignal.setCode(SIGNAL_UNKNOWN);
            this.tv_expSignalBlock.setText("---");
        }

        this.lastExpSpeed = this.engine.expSpeed;
        this.lastExpSignalCode = this.engine.expSignalCode;

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
                if (!this.engine.function[i].name.isEmpty())
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
     * Show DCC status
     */
    private void observeDccState() {
        TCPClient.getInstance().dccState.observe(this, state -> {
            boolean goEnabled = (state != null) && (!state.on) && (state.iCanMakeItGo);
            boolean stopEnabled = (state != null) && (state.on);

            this.ib_dcc_go.setEnabled(goEnabled);
            if (goEnabled) {
                this.ib_dcc_go.getDrawable().setColorFilter(null);
            } else {
                this.ib_dcc_go.getDrawable().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
            }

            this.ib_dcc_stop.setEnabled(stopEnabled);
            if (stopEnabled) {
                this.ib_dcc_stop.clearAnimation();
            } else {
                Animation blink = new AlphaAnimation(0.0f, 1.0f);
                blink.setDuration(100);
                blink.setRepeatMode(Animation.REVERSE);
                blink.setRepeatCount(Animation.INFINITE);
                this.ib_dcc_stop.startAnimation(blink);
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

    public void ib_dccGoClick(View v) {
        TCPClient.getInstance().send("-;DCC;GO");
    }

    public void ib_dccStopClick(View v) {
        new AlertDialog.Builder(this)
            .setMessage(R.string.ta_really_stop_dcc)
            .setNegativeButton(R.string.No, (dialog, which) -> {})
            .setPositiveButton(R.string.Yes, (dialog, which) -> {
                TCPClient.getInstance().send("-;DCC;STOP");
            })
            .show();
    }

    public void ib_ReleaseClick(View v) {
        if (engine == null)
            return;

        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.ta_release_really) + " " + this.engine.getTitle() + "?")
            .setPositiveButton(getString(R.string.yes), (dialog, which) -> this.engine.release())
            .setNegativeButton(getString(R.string.no), (dialog, which) -> {}).show();
    }

    private void displayGroupDialog(String title) {
        final ArrayList<Engine> engines = new ArrayList<>(EngineDb.instance.engines.values());
        Collections.sort(engines, (engine1, engine2) -> engine1.addr - engine2.addr);
        final CharSequence[] enginesTitle = new CharSequence[engines.size()];
        final boolean[] enginesChecked = new boolean[engines.size()];
        for (int i = 0; i < engines.size(); i++) {
            enginesTitle[i] = engines.get(i).getTitle();
            enginesChecked[i] = true;
        }

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMultiChoiceItems(enginesTitle, enginesChecked, (dialog, index, check) ->
                enginesChecked[index] = check
            )
            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                for (int j = 0; j < enginesChecked.length; j++) {
                    Engine _engine = engines.get(j);
                    _engine.multitrack = enginesChecked[j];
                    if ((!_engine.total) && (_engine.multitrack))
                        _engine.setTotal(true);
                }
                this.updateGUIFromTrain();
            })
            .setNegativeButton(R.string.cancel, (dialog, which) -> {})
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

    private void displayUntotalDialog() {
        final ArrayList<Engine> engines = new ArrayList<>(EngineDb.instance.engines.values());
        engines.remove(this.engine);
        Collections.sort(engines, (engine1, engine2) -> engine1.addr - engine2.addr);
        final CharSequence[] enginesTitle = new CharSequence[engines.size()];
        final boolean[] enginesChecked = new boolean[engines.size()];
        for (int i = 0; i < engines.size(); i++) {
            enginesTitle[i] = engines.get(i).getTitle();
            enginesChecked[i] = engines.get(i).multitrack;
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.ta_dialog_untotal_title)
            .setMultiChoiceItems(enginesTitle, enginesChecked, (dialog, index, check) ->
                enginesChecked[index] = check
            )
            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                for (int j = 0; j < enginesChecked.length; j++) {
                    Engine _engine = engines.get(j);
                    if ((enginesChecked[j]) && (_engine.total)) {
                        _engine.setTotal(false);
                        _engine.multitrack = false;
                    }
                }
            })
            .setNegativeButton(R.string.cancel, (dialog, which) -> {})
            .show();
    }

    private void gotoEngineRequestActivity() {
        this.startActivity(new Intent(this, EngineRequest.class));
        this.finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EngineChangeEvent event) {
        super.onEventMainThread(event);
        if ((this.engine != null) && (event.getAddr() == this.engine.addr))
            this.updateGUIFromTrain();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EngineAddEvent event) {
        super.onEventMainThread(event);
        this.updateGUIFromTrain(); // to enable multitraction checkbox
        if ((event.getAddr() != this.engine.addr) && (this.engine.total))
            this.displayGroupDialog(getString(R.string.ta_dialog_new_engine_group_title));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EngineRemoveEvent event) {
        super.onEventMainThread(event);
        this.updateGUIFromTrain(); // to disable multitraction checkbox

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

    private void notifyUser() {
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        if ((am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && (this.infoPlayer != null) && (!this.infoPlayer.isPlaying()))
            this.infoPlayer.start();

        if (am.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            final long VIBRATE_DURATION_MS = 200;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.vibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                this.vibrator.vibrate(VIBRATE_DURATION_MS);
            }
        }
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

        // It takes 3.9 s for train in H0 at 40 km/h to pass 50 cm (sensor-signal distance)
        // -> 3 s should be safe for stopping before the signal
        static final int OVERSPEED_DELAY_EB_MS = 3000;
        static final int DIR_MISMATCH_DELAY_EB_MS = 3000;
        public enum Mode {
            TRAIN,
            SHUNT,
        }

        public Mode mode = Mode.TRAIN;
        private boolean overspeed;
        private boolean dirMismatch;
        private MediaPlayer soundPlayer;
        private boolean warning = false;

        Handler t_overSpeedEB = new Handler(); // EB = emergency braking
        Runnable t_overSpeedEBRunnable = new Runnable() {
            @Override
            public void run() { overSpeedEB(); }
        };

        Handler t_dirMismatchEB = new Handler(); // EB = emergency braking
        Runnable t_dirMismatchEBRunnable = new Runnable() {
            @Override
            public void run() { dirMismatchEB(); }
        };

        public ATP() {
            try {
                this.soundPlayer = new MediaPlayer();
                final Context appContent = EngineController.this.getApplicationContext();
                final AssetFileDescriptor afd = appContent.getResources().openRawResourceFd(R.raw.s2_warning);
                this.soundPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                this.soundPlayer.setLooping(true);
                this.soundPlayer.prepareAsync();
            } catch (IOException e) {
                this.soundPlayer = null;
                Log.e("ATP ctor", "s2_warning sound load", e);
            }
        }

        public void onDestroy() {
            if (this.overspeed)
                this.overSpeedEnd();
            if (this.dirMismatch)
                this.dirMismatchEnd();
        }

        public void update() {
            this.overSpeedUpdate();
            this.directionUpdate();
        }

        private void warningStart() {
            AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

            if ((am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && (this.soundPlayer != null) && (!this.soundPlayer.isPlaying())) {
                if (EngineController.this.infoPlayer.isPlaying()) {
                    EngineController.this.infoPlayer.stop();
                    EngineController.this.infoPlayer.prepareAsync();
                }
                this.soundPlayer.start();
            }

            if (am.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                final long VIBRATE_DURATION_MS = 400;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    EngineController.this.vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, VIBRATE_DURATION_MS}, 0));
                } else {
                    EngineController.this.vibrator.vibrate(VIBRATE_DURATION_MS);
                }
            }

            this.warning = true;
        }

        private void warningCheckStop() {
            if ((!this.overspeed) && (!this.dirMismatch)) {
                if ((this.soundPlayer != null) && (this.soundPlayer.isPlaying())) {
                    this.soundPlayer.stop();
                    this.soundPlayer.prepareAsync();
                }
                EngineController.this.vibrator.cancel();
                this.warning = false;
            }
        }

        private void removeAllEBCallbacks() {
            this.t_overSpeedEB.removeCallbacks(t_overSpeedEBRunnable);
            this.t_dirMismatchEB.removeCallbacks(t_dirMismatchEBRunnable);
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
            this.t_overSpeedEB.postDelayed(t_overSpeedEBRunnable, OVERSPEED_DELAY_EB_MS);

            { // Animation
                Animation blink = new AlphaAnimation(0.0f, 1.0f);
                blink.setDuration(100);
                blink.setRepeatMode(Animation.REVERSE);
                blink.setRepeatCount(Animation.INFINITE);
                EngineController.this.tv_kmhSpeed.startAnimation(blink);
                EngineController.this.tv_expSpeed.startAnimation(blink);
            }

            this.warningStart();
        }

        private void overSpeedEnd() {
            this.t_overSpeedEB.removeCallbacks(t_overSpeedEBRunnable);
            EngineController.this.tv_kmhSpeed.clearAnimation();
            EngineController.this.tv_expSpeed.clearAnimation();
            this.warningCheckStop();
        }

        private void overSpeedEB() {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(EngineController.this);
            this.removeAllEBCallbacks();

            if ((EngineController.this.engine.isMyControl()) && (!sharedPreferences.getBoolean("ATPEBDisable", false))) {
                EngineController.this.emergencyStop();

                new AlertDialog.Builder(EngineController.this)
                    .setMessage(getString(R.string.ta_atp_overspeed_eb_msg))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {})
                    .show();
            }
        }

        private void directionUpdate() {
            final Engine thisEngine = EngineController.this.engine;

            boolean dirMismatch = false;
            if ((thisEngine.isMyControl()) && (this.mode != Mode.SHUNT) && (thisEngine.stepsSpeed > 0)) {
                dirMismatch = (thisEngine.expDirection != Engine.ExpDirection.UNKNOWN) ? (!thisEngine.expDirectionMatch(thisEngine.direction)) : false;
            }
            this.dirMismatchSet(dirMismatch);
        }

        private void dirMismatchSet(boolean mismatch) {
            if (mismatch == this.dirMismatch)
                return;
            this.dirMismatch = mismatch;

            if (mismatch)
                this.dirMismatchBegin();
            else
                this.dirMismatchEnd();
        }

        private void dirMismatchBegin() {
            this.t_dirMismatchEB.postDelayed(t_dirMismatchEBRunnable, DIR_MISMATCH_DELAY_EB_MS);

            { // Animation
                Animation blink = new AlphaAnimation(0.0f, 1.0f);
                blink.setDuration(100);
                blink.setRepeatMode(Animation.REVERSE);
                blink.setRepeatCount(Animation.INFINITE);
                EngineController.this.s_direction.startAnimation(blink);
                EngineController.this.tv_expDirection.startAnimation(blink);
            }

            this.warningStart();
        }

        private void dirMismatchEnd() {
            this.t_dirMismatchEB.removeCallbacks(t_dirMismatchEBRunnable);
            EngineController.this.s_direction.clearAnimation();
            EngineController.this.tv_expDirection.clearAnimation();
            this.warningCheckStop();
        }

        private void dirMismatchEB() {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(EngineController.this);
            this.removeAllEBCallbacks();

            if ((EngineController.this.engine.isMyControl()) && (!sharedPreferences.getBoolean("ATPEBDisable", false))) {
                EngineController.this.emergencyStop();

                new AlertDialog.Builder(EngineController.this)
                        .setMessage(getString(R.string.ta_atp_direction_eb_msg))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {})
                        .show();
            }
        }

        public boolean isMovementStartAllowed() {
            final Engine thisEngine = EngineController.this.engine;
            if (this.mode == Mode.SHUNT)
                return true;
            return (((thisEngine.expSpeed == EXP_SPEED_UNKNOWN) || (thisEngine.expSpeed > 0)) &&
                    (thisEngine.expDirectionMatch(thisEngine.direction)));
        }

        public boolean isWarning() {
            return this.warning;
        }
    }
}
