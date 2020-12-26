package cz.mendelu.xmarik.train_manager.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import cz.mendelu.xmarik.train_manager.network.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.events.AreasParsedEvent;
import cz.mendelu.xmarik.train_manager.events.LokAddEvent;
import cz.mendelu.xmarik.train_manager.events.TCPDisconnectEvent;
import cz.mendelu.xmarik.train_manager.models.ControlArea;
import cz.mendelu.xmarik.train_manager.events.RequestEvent;

public class TrainRequest extends NavigationBase {

    ArrayAdapter<String> lAdapter;
    Button sendButton;
    EditText messageForServer;
    int focused;
    AlertDialog.Builder connectionDialog;
    Dialog dialog;
    TextView dialogMessage;
    Button dialogButton;
    View lastSelected;

    ListView areas_lv;
    ArrayList<String> areas_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_train_request);
        super.onCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_train_request);
        dialog.setTitle(R.string.tr_request);
        dialogMessage = dialog.findViewById(R.id.dialogMessage);
        dialogButton = dialog.findViewById(R.id.cancelButton);
        dialogButton.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(dialogInterface -> cancelRequest());

        connectionDialog = new AlertDialog.Builder(this);

        areas_lv = findViewById(R.id.nav_areas);
        sendButton = findViewById(R.id.b_request);
        messageForServer = findViewById(R.id.authMessage);
        focused = -1;

        areas_data = new ArrayList<>();
        lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, areas_data);

        areas_lv.setOnItemClickListener((parent, view, position, id) -> {
            int c = ColorUtils.setAlphaComponent(getResources().getColor(R.color.colorPrimary), 0x44);
            view.setBackgroundColor(c);
            if (lastSelected != null && !lastSelected.equals(view))
                lastSelected.setBackgroundColor(0); // transparent color
            lastSelected = view;
            focused = position;
        });

        messageForServer.setOnFocusChangeListener((view, b) -> {
            if (messageForServer.isFocused()) messageForServer.setText("");
        });

        areas_lv.setAdapter(lAdapter);
    }

    void FillAreas() {
        focused = -1;
        if (lastSelected != null) {
            lastSelected.setBackgroundColor(0); // transparent color
            lastSelected = null;
        }

        areas_data.clear();

        areas_lv.setEnabled(ControlAreaDb.instance.areas.size() > 0);
        if (ControlAreaDb.instance.areas.size() == 0)
            areas_data.add(getString(R.string.tr_no_areas));

        for(ControlArea c : ControlAreaDb.instance.areas)
            areas_data.add(c.name);

        lAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LokAddEvent event) {
        startActivity(new Intent(this, TrainHandler.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RequestEvent event) {
        if (event.getParsed().get(4).toUpperCase().equals("OK")) {
            dialogMessage.setText(R.string.tr_info_waiting_disp);
        } else if (event.getParsed().get(4).toUpperCase().equals("ERR")) {
            new AlertDialog.Builder(this)
                    .setMessage(event.getParsed().size() >= 6 ? event.getParsed().get(5) : getString(R.string.general_error))
                    .setCancelable(false)
                    .setPositiveButton("ok", (dialog, which) -> {} ).show();
            dialog.dismiss();
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
    public void onEventMainThread(TCPDisconnectEvent event) {
        dialog.dismiss();
        super.onEventMainThread(event);
    }

    public void b_requestClick(View v) {
        if (areas_lv.getItemAtPosition(focused) == null) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.tr_no_area_selected))
                    .setCancelable(false)
                    .setPositiveButton("ok", (dialog, which) -> {} ).show();
            return;
        }

        TCPClientApplication.getInstance().send("-;LOK;G;PLEASE;" +
                ControlAreaDb.instance.areas.get(focused).id + ";" + messageForServer.getText().toString());

        dialog.setTitle(getString(R.string.tr_info_request_requesting));
        dialogMessage.setText(R.string.tr_info_request_sent);
        dialog.show();
    }

    private void cancelRequest() {
        TCPClientApplication.getInstance().send("-;LOK;G;CANCEL");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            EventBus.getDefault().unregister(this);
            dialog.dismiss();
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        dialog.dismiss();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        FillAreas();
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

}
