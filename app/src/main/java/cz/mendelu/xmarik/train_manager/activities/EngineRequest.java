package cz.mendelu.xmarik.train_manager.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.AreasClearedEvent;
import cz.mendelu.xmarik.train_manager.storage.ControlAreaDb;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.network.TCPClient;
import cz.mendelu.xmarik.train_manager.events.AreasParsedEvent;
import cz.mendelu.xmarik.train_manager.events.EngineAddEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectedEvent;
import cz.mendelu.xmarik.train_manager.models.ControlArea;
import cz.mendelu.xmarik.train_manager.events.RequestEvent;

public class EngineRequest extends NavigationBase {

    ArrayAdapter<String> lAdapter;
    EditText messageForServer;
    Dialog dialog;
    TextView dialogMessage;

    ListView areas_lv;
    ArrayList<String> areas_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_engine_request);
        super.onCreate(savedInstanceState);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final View dialogView = this.getLayoutInflater().inflate(R.layout.dialog_engine_request, null);
        this.dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        this.dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener(dialogInterface -> cancelRequest())
                .create();

        this.areas_lv = findViewById(R.id.nav_areas);
        this.messageForServer = findViewById(R.id.authMessage);

        this.areas_data = new ArrayList<>();
        this.lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, areas_data);

        this.areas_lv.setOnItemClickListener((parent, v, position, id) -> onLvAreasItemClick(parent, v, position, id));
        this.areas_lv.setAdapter(lAdapter);

        this.messageForServer.setOnFocusChangeListener((view, b) -> {
            if (messageForServer.isFocused())
                messageForServer.setText("");
        });
    }

    public void onLvAreasItemClick(AdapterView<?> parent, View v, int position, long id) {
        final ControlArea area = ControlAreaDb.instance.areas.get(position);
        TCPClient.getInstance().send("-;LOK;G;PLEASE;" + area.id + ";" +
                messageForServer.getText().toString());

        // If disconnect happens as a reaction to send, do not show dialog
        if (TCPClient.getInstance().connected()) {
            dialog.setTitle(area.name);
            dialogMessage.setText(R.string.tr_info_request_sent);
            dialog.show();
        }
    }

    void FillAreas() {
        this.areas_data.clear();

        this.areas_lv.setEnabled(ControlAreaDb.instance.areas.size() > 0);
        if (ControlAreaDb.instance.areas.size() == 0)
            this.areas_data.add(getString(R.string.tr_no_areas));

        for(ControlArea c : ControlAreaDb.instance.areas)
            this.areas_data.add(c.name);

        this.lAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EngineAddEvent event) {
        super.onEventMainThread(event);

        final Intent intent = (new Intent(this, EngineController.class));
        intent.putExtra("train_addr", event.getAddr());
        this.startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RequestEvent event) {
        if (event.getParsed().get(4).equalsIgnoreCase("OK")) {
            this.dialogMessage.setText(R.string.tr_info_waiting_disp);
        } else if (event.getParsed().get(4).equalsIgnoreCase("ERR")) {
            new AlertDialog.Builder(this)
                    .setMessage(event.getParsed().size() >= 6 ? event.getParsed().get(5) : getString(R.string.general_error))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {} ).show();
            this.dialog.dismiss();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AreasParsedEvent event) {
        this.FillAreas();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AreasClearedEvent event) {
        this.FillAreas();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TCPDisconnectedEvent event) {
        super.onEventMainThread(event);
        this.dialog.dismiss();
        this.finish();
    }

    private void cancelRequest() {
        TCPClient.getInstance().send("-;LOK;G;CANCEL");
    }

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            EventBus.getDefault().unregister(this);
            this.dialog.dismiss();
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        // Called when other activity comes to foreground
        // Not called when dialog comes to foreground
        super.onPause();
        this.dialog.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.FillAreas();
        if (!TCPClient.getInstance().connected())
            this.finish();
    }

}
