package cz.mendelu.xmarik.train_manager.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.StoredServersReloadEvent;
import cz.mendelu.xmarik.train_manager.helpers.HashHelper;
import cz.mendelu.xmarik.train_manager.models.Server;
import cz.mendelu.xmarik.train_manager.storage.ServerDb;

public class ServerSelectStored extends Fragment {

    ArrayAdapter<String[]> adapter;
    final ArrayList<String[]> servers = new ArrayList<>();
    ListView lvServers;
    View view;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState
    ) {
        view = inflater.inflate(R.layout.content_server_select_stored, container);

        lvServers = view.findViewById(R.id.servers);

        adapter = new ArrayAdapter<String[]>(view.getContext(),
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
        lvServers.setAdapter(adapter);

        registerForContextMenu(lvServers);

        lvServers.setOnItemClickListener((parent, view, position, id) -> {
            if (ServerDb.instance.stored.get(position).active) {
                connect(position);
            } else {
                new AlertDialog.Builder(this.view.getContext())
                        .setMessage(R.string.conn_server_offline)
                        .setPositiveButton(getString(R.string.yes), (dialog, __) -> connect(position))
                        .setNegativeButton(getString(R.string.no), (dialog, __) -> {}).show();
            }
        });

        return view;
    }

    public void updateServers() {
        servers.clear();
        for (Server s : ServerDb.instance.stored) {
            servers.add(new String[]{
                    s.getTitle(),
                    s.type
            });
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    public void connect(int index) {
        Intent intent = new Intent(view.getContext(), ServerConnector.class);
        intent.putExtra("serverType", "stored");
        intent.putExtra("serverId", index);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.servers) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(ServerDb.instance.stored.get(info.position).host);

            String[] menuItems = {
                    getString(R.string.mm_connect),
                    getString(R.string.mm_change_login),
                    getString(R.string.mm_change_settings),
                    getString(R.string.mm_delete),
                    getString(R.string.mm_delete_all)
            };

            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Context context = view.getContext().getApplicationContext();
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        Server s = ServerDb.instance.stored.get(info.position);

        switch (menuItemIndex) {
            case 0:
                if (s.active) {
                    connect(info.position);
                } else {
                    new AlertDialog.Builder(view.getContext())
                        .setMessage(R.string.conn_server_offline)
                        .setPositiveButton(getString(R.string.yes), (dialog, __)
                                -> connect(info.position))
                        .setNegativeButton(getString(R.string.no), (dialog, __)
                                -> {}).show();
                }
                break;

            case 1:
                changeLogin(s);
                break;

            case 2:
                Intent intent = new Intent(context, ServerEdit.class);
                intent.putExtra("serverId", info.position);
                startActivity(intent);
                break;

            case 3:
                new AlertDialog.Builder(view.getContext())
                        .setMessage(R.string.conn_delete_server)
                        .setPositiveButton(getString(R.string.yes), (dialog, __)
                                -> ServerDb.instance.removeStoredServer(info.position))
                        .setNegativeButton(getString(R.string.no), (dialog, __)
                                -> {}).show();
                break;

            case 4:
                new AlertDialog.Builder(view.getContext())
                        .setMessage(R.string.conn_delete_all)
                        .setPositiveButton(getString(R.string.yes), (dialog, __)
                                -> ServerDb.instance.clearStoredServers())
                        .setNegativeButton(getString(R.string.no), (dialog, __)
                                -> {}).show();
                break;
        }
        return true;
    }

    public void changeLogin(final Server server) {
        View dialogView = this.getLayoutInflater().inflate(R.layout.dialog_user, null);
        final EditText mName = dialogView.findViewById(R.id.dialogName);
        final EditText mPasswd = dialogView.findViewById(R.id.dialogPasswd);
        final CheckBox savebox = dialogView.findViewById(R.id.dialogSaveData);

        mName.setText(server.username);
        savebox.setVisibility(View.GONE);

        new AlertDialog.Builder(view.getContext())
                .setView(dialogView)
                .setTitle(R.string.mm_change_login)
                .setPositiveButton(R.string.dialog_ok, (dialog1, which) -> {
                    server.username = mName.getText().toString().trim();
                    server.password = HashHelper.hashPasswd(mPasswd.getText().toString());
                    ServerDb.instance.transferLoginToFound(server);
                })
                .setNegativeButton(R.string.cancel, (dialog1, which) -> {
                    dialog1.cancel();
                })
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StoredServersReloadEvent event) {
        updateServers();
    }

    @Override
    public void onPause() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        updateServers();
    }
}
