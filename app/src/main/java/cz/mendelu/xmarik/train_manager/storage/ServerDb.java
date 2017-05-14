package cz.mendelu.xmarik.train_manager.storage;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.events.FoundServersReloadEvent;
import cz.mendelu.xmarik.train_manager.events.StoredServersReloadEvent;
import cz.mendelu.xmarik.train_manager.models.Server;

/**
 * Class for in memmory store servers, during run.
 */
public class ServerDb {
    public static ServerDb instance;
    public ArrayList<Server> found;
    public ArrayList<Server> stored;

    public ServerDb() {
        this.found = new ArrayList<>();
        this.stored = new ArrayList<>();
    }

    public String getSaveString() {
        String saveString = "";
        for (Server s : this.stored) {
            saveString = saveString + s.getSaveDataString() + "|";
        }
        return saveString;
    }

    public void loadServers(String servers) {
        String[] serverString = servers.split("\\|");
        for (String tmpS : serverString) {
            String[] attributes = tmpS.split(";");
            if (tmpS.length() > 5) {
                Server tmpServer = new Server(attributes[0], attributes[1], Integer.parseInt(attributes[2]), false,
                        attributes[3], attributes[4], attributes[5]);
                if (!stored.contains(tmpServer)) stored.add(tmpServer);
            }
        }
    }

    public void clearStoredServers() {
        this.stored.clear();
        EventBus.getDefault().post(new StoredServersReloadEvent());
    }

    public void clearFoundServers() {
        this.found.clear();
        EventBus.getDefault().post(new FoundServersReloadEvent());
    }

    public void addStoredServer(Server server) {
        this.stored.add(server);
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
            this.stored.remove(position);
            EventBus.getDefault().post(new StoredServersReloadEvent());
        }
    }

    public boolean isFoundServer(String host, int port) {
        for (Server s : found)
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
    }

}
