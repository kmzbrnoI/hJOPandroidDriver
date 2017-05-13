package cz.mendelu.xmarik.train_manager.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cz.mendelu.xmarik.train_manager.models.CustomServerComparator;
import cz.mendelu.xmarik.train_manager.models.Server;

/**
 * Class for in memmory store servers, during run.
 */
public class ServerDb {

    private static ServerDb instance;
    private ArrayList<Server> servers;
    private ArrayList<Server> storedServers;

    protected ServerDb() {
        this.servers = new ArrayList<>();
        this.storedServers = new ArrayList<>();
    }

    public static ServerDb getInstance() {
        if (instance == null) instance = new ServerDb();
        return instance;
    }

    public void setPassword(Server server) {
        if (storedServers.contains(server)) {
            for (Server s : storedServers) {
                if (s.equals(server)) {
                    s.password = server.password;
                    s.username = server.username;
                }
            }
        } else storedServers.add(server);
    }

    public String getSaveString() {
        String saveString = "";
        for (Server s : this.storedServers) {
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
                if (!storedServers.contains(tmpServer)) storedServers.add(tmpServer);
            }
        }
    }

    public void clearCustomServer() {
        Server tmp = this.getActiveServer();
        this.storedServers.clear();
        if (tmp != null) this.storedServers.add(tmp);
    }

    public void addServer(ArrayList<Server> servers) {
        for(Server s : servers) {
            if(!this.servers.contains(s))
                this.servers.add(s);
        }
    }

    public void clear() {
        Set<Server> hs = new HashSet<>();
        hs.addAll(servers);
        servers.clear();
        servers.addAll(hs);
    }

    public void addCustomServer(Server server) {
        if (!this.storedServers.contains(server)) {
                this.storedServers.add(server);
                Collections.sort(storedServers, new CustomServerComparator());
        }
    }

    public ArrayList<Server> getServers() {
        return this.servers;
    }

    public ArrayList<String> getServersString() {
        ArrayList<String> tmp = new ArrayList<>();
        for (Server s : this.servers) {
            tmp.add(s.getStringData());
        }
        return tmp;
    }

    public ArrayList<String> getStoredServersString() {
        ArrayList<String> tmp = new ArrayList<>();
        for (Server s : this.storedServers) {
            tmp.add(s.getStoredStringData());
        }
        return tmp;
    }

    public ArrayList<Server> getCustomServers() {
        return this.storedServers;
    }

    public Server getActiveServer() {
        for (Server s : this.servers) {
            if (s.active) return s;
        }
        for (Server s : this.storedServers) {
            if (s.active) return s;
        }
        return null;
    }

    public void deleteAllData() {
        storedServers = null;
        servers = null;
    }

    public void deleteAllUserData() {
        for (Server s : this.storedServers) {
            s.username = "";
            s.password = "";
        }
        for (Server s : this.servers) {
            s.username = "";
            s.password = "";
        }
    }

    public Server getServer(String itemValue) {
        for (Server s : this.storedServers) {
            if (s.name.equals(itemValue)) return s;
        }
        for (Server s : servers) {
            if (s.name.equals(itemValue)) return s;
        }
        return null;
    }

    public void removeServer(int position) {
        if (position <= storedServers.size())
            this.storedServers.remove(position);
    }

    public String getServerStoreString() {
        String serverStoreString = "";
        for (Server s : this.storedServers) {
            serverStoreString = serverStoreString + "--" + s.getStringData();
        }
        return serverStoreString;
    }

    public void setActive(Server server) {
        for (Server s : this.servers) {
            if (s.active) {
                if (!s.equals(server)) {
                    s.active = false;
                }
            }
        }
        for (Server s : this.storedServers) {
            if (s.active) {
                if (!s.equals(server)) {
                    s.active = false;
                }
            }
        }
    }

    public void deactivateServer() {
        for (Server s : this.servers) {
            if (s.active) {
                s.active = false;
            }
        }
        for (Server s : this.storedServers) {
            if (s.active) {
                s.active = false;
            }
        }
    }

    public void clearLocalServers() {
        this.servers = new ArrayList<>();
    }

}