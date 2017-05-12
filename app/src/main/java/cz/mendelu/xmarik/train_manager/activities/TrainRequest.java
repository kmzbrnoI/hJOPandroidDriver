package cz.mendelu.xmarik.train_manager.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.ControlAreaDb;
import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.models.ControlArea;
import cz.mendelu.xmarik.train_manager.events.RequestEvent;

public class TrainRequest extends NavigationBase {

    Context context;
    ProgressBar mProgressBar;
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

        EventBus.getDefault().register(this);
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
                cancelMessage();
            }
        });

        connectionDialog = new AlertDialog.Builder(this);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        areas_lv = (ListView) findViewById(R.id.nav_areas);
        sendButton = (Button) findViewById(R.id.b_request);
        messageForServer = (EditText) findViewById(R.id.authMessage);
        focused = -1;
        mProgressBar.setVisibility(View.GONE);

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

        FillAreas();
    }

    void FillAreas() {
        areas_data.clear();

        areas_lv.setEnabled(ControlAreaDb.instance.areas.size() > 0);
        if (ControlAreaDb.instance.areas.size() == 0)
            areas_data.add("No areas!"); // TODO: string here

        for(ControlArea c : ControlAreaDb.instance.areas)
            areas_data.add(c.name);
    }

    /*private void sendNext(String message) {
        //sends the message to the server
        if (TCPClientApplication.getInstance() != null) {
            TCPClientApplication.getInstance().send(message);
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }*/

    /*@Subscribe
    public void onEvent(TrainReloadEvent event) {
        // your implementation
        reloadEventHelper();
        if (this.sendButton.getText().equals(R.string.cancel)) this.sendButton.setText(R.string.tr_send_request);
        Toast.makeText(getApplicationContext(),
                R.string.gl_new_loko, Toast.LENGTH_LONG)
                .show();
        //TODO podminka s automatickym prechodem
        EventBus.getDefault().unregister(this);
        dialog.dismiss();
        Intent intent = new Intent(this, TrainHandler.class);
        startActivity(intent);
    }*/

    @Subscribe
    public void onEvent(RequestEvent event) {
        // TODO
        /*dialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(event.getParsed())
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

        mProgressBar.setVisibility(View.GONE);
        this.sendButton.setText(R.string.tr_send_request);*/
    }

   /* @Subscribe
    public void onEvent(FreeEvent event) {
        Toast.makeText(getApplicationContext(),
                "loko uvolneno", Toast.LENGTH_LONG)
                .show();
        reloadEventHelper();
        this.sendButton.setText(R.string.poslatZ);
    }*/

    /*@Subscribe
    public void onEvent(ServerOkEvent event) {
        dialogMessage.setText(R.string.tr_info_waiting_disp);
    }*/

    /**
     * method for handl send button event pressed
     *
     * @param v
     */
    public void b_requestClick(View v) {
        if (areas_lv.getItemAtPosition(focused) == null) {
            new AlertDialog.Builder(this)
                    .setMessage("You must select area!") // TODO: strings
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
        mProgressBar.setVisibility(View.VISIBLE);
        dialogMessage.setText(R.string.tr_info_request_sent);
        dialog.show();
    }

    private void cancelMessage() {
        /*sendNext("-;LOK;G;CANCEL");
        mProgressBar.setVisibility(View.GONE);
        this.sendButton.setText(R.string.tr_send_request);
        this.trains.setClickable(true);*/
    }


    /**
     * when there is new loko in server answer, this method will be called
     * purpose is reload all list adapter and make progress bar invisible
     */
    private void reloadEventHelper() {
        /*mProgressBar.setVisibility(View.GONE);
        final ArrayList<String> array;
        final ArrayList<String> acquired;
        array = active.getUnAuthorizedAreas();
        acquired = active.getAuthorizedTrainsString();
        final ArrayAdapter<String> lAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, array);

        final ArrayAdapter<String> ackAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, acquired);

        trains.setAdapter(lAdapter);*/
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
        //connectionDialog.dismiss();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

}
