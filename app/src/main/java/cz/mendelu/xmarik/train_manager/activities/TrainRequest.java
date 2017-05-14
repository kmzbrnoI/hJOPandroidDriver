package cz.mendelu.xmarik.train_manager.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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

    Context context;
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_train_request);
        dialog.setTitle(R.string.tr_request);
        dialogMessage = (TextView) dialog.findViewById(R.id.dialogMessage);
        dialogButton = (Button) dialog.findViewById(R.id.cancelButton);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                cancelRequest();
            }
        });

        connectionDialog = new AlertDialog.Builder(this);

        areas_lv = (ListView) findViewById(R.id.nav_areas);
        sendButton = (Button) findViewById(R.id.b_request);
        messageForServer = (EditText) findViewById(R.id.authMessage);
        focused = -1;

        areas_data = new ArrayList<String>();
        lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, areas_data);

        areas_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                view.setBackgroundColor(Color.rgb(153, 204, 255));
                if (lastSelected != null && !lastSelected.equals(view)) {
                    lastSelected.setBackgroundColor(Color.rgb(238, 238, 238));
                }
                lastSelected = view;
                focused = position;
            }
        });

        messageForServer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (messageForServer.isFocused()) messageForServer.setText("");
            }
        });

        areas_lv.setAdapter(lAdapter);
    }

    void FillAreas() {
        focused = -1;
        if (lastSelected != null) {
            lastSelected.setBackgroundColor(Color.rgb(238, 238, 238));
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

    @Subscribe
    public void onEvent(LokAddEvent event) {
        startActivity(new Intent(this, TrainHandler.class));
    }

    @Subscribe
    public void onEvent(RequestEvent event) {
        if (event.getParsed().get(4).toUpperCase().equals("OK")) {
            dialogMessage.setText(R.string.tr_info_waiting_disp);
        } else if (event.getParsed().get(4).toUpperCase().equals("ERR")) {
            new AlertDialog.Builder(this)
                    .setMessage(event.getParsed().size() >= 6 ? event.getParsed().get(5) : getString(R.string.general_error))
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
            dialog.dismiss();
        }
    }

    @Subscribe
    public void onEvent(AreasParsedEvent event) {
        this.FillAreas();
    }

    @Subscribe
    public void onEvent(AreasClearedEvent event) {
        this.FillAreas();
    }

    @Subscribe
    public void onEvent(TCPDisconnectEvent event) {
        dialog.dismiss();

        // TODO:  move this to parent object?
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.disconnected))
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(getBaseContext(), ServerSelect.class));
                    }
                }).show();
    }

    public void b_requestClick(View v) {
        if (areas_lv.getItemAtPosition(focused) == null) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.tr_no_area_selected))
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

}
