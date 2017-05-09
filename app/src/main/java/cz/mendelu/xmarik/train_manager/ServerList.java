package cz.mendelu.xmarik.train_manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cz.mendelu.xmarik.train_manager.models.Server;

/**
 * Created by ja on 15. 6. 2016.
 * Class for in memmory store servers, during run
 */
public class ServerList {

    public static String[] FUNCTION = {"F0", "F1", "F2", "F3",
            "F4", "F5", "F6", "F7",
            "F8", "F9", "F10", "F11",
            "F12", "F13", "F14", "F15",
            "F16", "F17", "F18", "F19",
            "F20", "F21", "F22", "F23",
            "F24", "F25", "F26", "F27",};
    private static ServerList instance;
    private ArrayList<Server> servers;
    private ArrayList<Server> storedServers;

    protected ServerList() {
        this.servers = new ArrayList<>();
        this.storedServers = new ArrayList<>();
        //just fot testing
        /*this.servers.add(new Server("test1","127.1.2.2",1448,false, ""));
        this.storedServers.add(new Server("test2","192.168.2.101",4444,true, ""));
        this.storedServers.add(new Server("test3","192.168.0.114",4444,true, ""));
        this.storedServers.get(0).setActive(true);*/
    }

    public static ServerList getInstance() {
        if (instance == null) {
            instance = new ServerList();
        }
        //nacist stored servery
        return instance;
    }

    public void setPassword(Server server) {
        if (storedServers.contains(server)) {
            for (Server s : storedServers) {
                if (s.equals(server)) {
                    s.setUserPassword(server.getUserPassword());
                    s.setUserName(server.getUserName());
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
                Server tmpServer = new Server(attributes[0], attributes[1], attributes[2], attributes[3],
                        attributes[4], attributes[5]);
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
            if (s.getActive()) return s;
        }
        for (Server s : this.storedServers) {
            if (s.getActive()) return s;
        }
        return null;
    }

    public void deleteAllData() {
        storedServers = null;
        servers = null;
    }

    public void deleteAllUserData() {
        for (Server s : this.storedServers) {
            s.setUserName(null);
            s.setUserPassword(null);
        }
        for (Server s : this.servers) {
            s.setUserName(null);
            s.setUserPassword(null);
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
            if ((s.getActive())) {
                if (!s.equals(server)) {
                    s.setActive(false);
                }
            }
        }
        for (Server s : this.storedServers) {
            if ((s.getActive())) {
                if (!s.equals(server)) {
                    s.setActive(false);
                }
            }
        }
    }

    public void deactivateServer() {
        for (Server s : this.servers) {
            if ((s.getActive())) {
                s.setActive(false);
            }
        }
        for (Server s : this.storedServers) {
            if ((s.getActive())) {
                s.setActive(false);
            }
        }
    }

    public void clearLocalServers() {
        this.servers = new ArrayList<>();
    }

    /**
     * enum type for loko engine
     */
    public enum TRAINTYPE {
        parní, dieslová, motorová, elektrická
    }

}
