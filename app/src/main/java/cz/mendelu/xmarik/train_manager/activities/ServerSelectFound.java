package cz.mendelu.xmarik.train_manager.activities;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.UDPDiscoveryFinishedEvent;
import cz.mendelu.xmarik.train_manager.events.UDPNewServerEvent;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.network.UDPDiscover;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;

public class ServerSelectFound extends Fragment {

    ArrayAdapter<String[]> adapterLvServers;
    final ArrayList<String[]> servers = new ArrayList<>();
    ListView lvServers;
    View view;
    UDPDiscover udpDiscover = null;
    Toast t_no_servers_found = null;

    SwipeRefreshLayout refreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState
    ) {
        this.view = inflater.inflate(R.layout.content_server_select_found, container);

        this.refreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        this.lvServers = view.findViewById(R.id.servers);

        adapterLvServers = new ArrayAdapter<String[]>(view.getContext(),
                android.R.layout.simple_list_item_2, android.R.id.text1, servers) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                text1.setText(servers.get(position)[0]);
                text2.setText(servers.get(position)[1]);
                return view;
            }
        };

        this.lvServers.setAdapter(adapterLvServers);
        this.lvServers.setOnItemClickListener((parent, view, position, id) -> lvServersOnItemClick(parent, view, position, id));

        this.t_no_servers_found = Toast.makeText(this.view.getContext().getApplicationContext(), R.string.conn_no_servers_found, Toast.LENGTH_SHORT);

        // bind SwipeRerfreshLayout
        this.refreshLayout.setOnRefreshListener(this::discoverServers);

        // run UDP discover after UI init:
        this.refreshLayout.post(this::discoverServers);

        return view;
    }

    private void lvServersOnItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (ServerDb.instance.found.get(position).active) {
            this.connect(position);
        } else {
            new AlertDialog.Builder(this.view.getContext())
                    .setMessage(R.string.conn_server_offline)
                    .setPositiveButton(getString(R.string.yes), (dialog, __) -> this.connect(position))
                    .setNegativeButton(getString(R.string.no), (dialog, __) -> {}).show();
        }
    }

    public void updateServers() {
        this.servers.clear();
        for (Server s : ServerDb.instance.found) {
            this.servers.add(new String[]{
                s.getTitle(),
                s.type + "\t" + (s.active ? "online" : "offline")
            });
        }
        this.adapterLvServers.notifyDataSetChanged();
    }

    public void discoverServers() {
        Context context = view.getContext().getApplicationContext();
        WifiManager wifiMgr = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        this.t_no_servers_found.cancel(); // hide potentially visible toast

        if (!isWifiOnAndConnected()) {
            this.refreshLayout.setRefreshing(false);
            Toast.makeText(context, R.string.conn_wifi_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        if ((this.udpDiscover != null) && (this.udpDiscover.isAlive())) {
            Toast.makeText(context, R.string.conn_refresh_in_progress, Toast.LENGTH_LONG).show();
            return;
        }

        ServerDb.instance.clearFoundServers();
        updateServers();

        this.refreshLayout.setRefreshing(true);
        this.udpDiscover = new UDPDiscover(wifiMgr);
        this.udpDiscover.start();
    }

    private boolean isWifiOnAndConnected() {
        Context context = view.getContext().getApplicationContext();
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            return (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED);
        }
        else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    public void connect(int index) {
        Intent intent = new Intent(view.getContext(), ServerConnector.class);
        intent.putExtra("serverType", "found");
        intent.putExtra("serverId", index);
        this.startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUdpNewServerEvent(UDPNewServerEvent event) {
        if (!ServerDb.instance.isFoundServer(event.server)) {
            ServerDb.instance.addFoundServer(event.server);
            this.updateServers();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUdpDiscoveryFinished(UDPDiscoveryFinishedEvent event) {
        this.refreshLayout.setRefreshing(false);
        if (ServerDb.instance.found.isEmpty())
            this.t_no_servers_found.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.updateServers();
    }
}
