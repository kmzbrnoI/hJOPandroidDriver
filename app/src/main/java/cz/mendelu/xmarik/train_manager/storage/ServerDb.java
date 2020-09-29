package cz.mendelu.xmarik.train_manager.storage;

import android.content.SharedPreferences;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.FoundServersReloadEvent;
import cz.mendelu.xmarik.train_manager.events.StoredServersReloadEvent;
import cz.mendelu.xmarik.train_manager.models.Server;

/**
 * Class for in memmory store servers, during run.
 */
public class ServerDb {
    SharedPreferences preferences;
    public static ServerDb instance;
    public ArrayList<Server> found;
    public ArrayList<Server> stored;

    public ServerDb(SharedPreferences preferences) {
        this.found = new ArrayList<>();
        this.stored = new ArrayList<>();

        this.preferences = preferences;
        this.loadServers();
    }

    public void loadServers() {
        if (!preferences.contains("StoredServers")) return;
        String[] serverString = preferences.getString("StoredServers", "").split("\\|");

        for (String tmpS : serverString) {
            try {
                String[] attributes = tmpS.split(";", -1);
                if (attributes.length > 5) {
                    Server tmpServer = new Server(attributes[0], attributes[1], Integer.parseInt(attributes[2]),
                            false, attributes[3], attributes[4], attributes[5]);
                    tmpServer.active = true;
                    if (!stored.contains(tmpServer)) stored.add(tmpServer);
                }
            } catch (Exception e){
                Log.e("ServerDb", "loadServers: " + e.getMessage());
            }
        }
    }

    public void saveServers() {
        StringBuilder saveString = new StringBuilder();
        for (Server s : this.stored)
            saveString.append(s.getSaveDataString()).append("|");

        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("StoredServers");
        editor.clear();
        editor.putString("StoredServers", saveString.toString());
        editor.commit();
    }

    public void clearStoredServers() {
        this.stored.clear();

        for (Server s : found) {
            s.username = "";
            s.password = "";
        }

        this.saveServers();
        EventBus.getDefault().post(new StoredServersReloadEvent());
    }

    public void clearFoundServers() {
        this.found.clear();
        EventBus.getDefault().post(new FoundServersReloadEvent());
    }

    public void addStoredServer(Server server) {
        this.stored.add(server);
        this.saveServers();
        EventBus.getDefault().post(new StoredServersReloadEvent());
    }

    public void addFoundServer(Server server) {
        this.found.add(server);
        EventBus.getDefault().post(new FoundServersReloadEvent());

        // transfer password from stored servers
        if (server.username.isEmpty() && server.password.isEmpty()) {
            for (Server s : stored) {
                if (server.host.equals(s.host) && server.port == s.port) {
                    server.username = s.username;
                    server.password = s.password;
                    break;
                }
            }
        }
    }

    public void removeStoredServer(int position) {
        if (position <= stored.size()) {
            for (Server s : found) {
                if (s.host.equals(stored.get(position).host) && s.port == stored.get(position).port) {
                    s.username = "";
                    s.password = "";
                }
            }

            this.stored.remove(position);
            this.saveServers();
            EventBus.getDefault().post(new StoredServersReloadEvent());
        }
    }

    public boolean isFoundServer(String host, int port) {
        for (Server s : found)
            if (s.host.equals(host) && s.port == port)
                return true;
        return false;
    }

    public boolean isStoredServer(String host, int port) {
        for (Server s : stored)
            if (s.host.equals(host) && s.port == port)
                return true;
        return false;
    }

    public void transferLoginToSaved(Server found) {
        for(Server s : stored) {
            if (s != found && s.host.equals(found.host) && s.port == found.port) {
                s.username = found.username;
                s.password = found.password;
            }
        }
        this.saveServers();
    }

}
