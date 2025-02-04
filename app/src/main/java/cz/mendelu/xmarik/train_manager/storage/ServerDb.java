package cz.mendelu.xmarik.train_manager.storage;

import android.content.SharedPreferences;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

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
        editor.apply();
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
    }

    public void addStoredServer(Server server) {
        this.stored.add(server);
        this.saveServers();
        EventBus.getDefault().post(new StoredServersReloadEvent());
    }

    public void addFoundServer(Server server) {
        this.found.add(server);

        // transfer password from stored servers
        if (server.username.isEmpty() && server.password.isEmpty()) {
            for (Server s : stored) {
                if (server.equals(s)) {
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
                if (s.equals(stored.get(position))) {
                    s.username = "";
                    s.password = "";
                }
            }

            this.stored.remove(position);
            this.saveServers();
            EventBus.getDefault().post(new StoredServersReloadEvent());
        }
    }

    public Server findFoundServer(Server server) {
        for (Server s : found)
            if (s.equals(server))
                return s;
        return null;
    }

    public boolean isFoundServer(Server server) {
        return (this.findFoundServer(server) != null);
    }

    public Server findStoredServer(Server server) {
        for (Server s : stored)
            if (s.equals(server))
                return s;
        return null;
    }

    public boolean isStoredServer(Server server) {
        return (this.findStoredServer(server) != null);
    }

    public void transferLoginToStored(Server found) {
        for(Server s : stored) {
            if ((s != found) && (s.equals(found))) {
                s.username = found.username;
                s.password = found.password;
            }
        }
        this.saveServers();
    }

    public void transferLoginToFound(Server stored) {
        for(Server s : found) {
            if ((s != stored) && (s.equals(stored))) {
                s.username = stored.username;
                s.password = stored.password;
            }
        }
    }
}
