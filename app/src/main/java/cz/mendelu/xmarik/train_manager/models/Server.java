package cz.mendelu.xmarik.train_manager.models;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cz.mendelu.xmarik.train_manager.ControlArea;
import cz.mendelu.xmarik.train_manager.TCPClient;
import cz.mendelu.xmarik.train_manager.TCPClientApplication;

/**
 * Class Server represents a single server. Server contains database of all trains.
 */
public class Server {
    public static AtomicInteger globalId = new AtomicInteger(0);

    public int id;
    public String name;
    public String host;
    public int port;
    public String type;
    public boolean active;

    public String username;
    public String password;

    public List<ControlArea> areas; // TODO: remove this

    public Server(String name, String host, int port, boolean active, String type, String username, String password) {
        this.id = globalId.incrementAndGet();
        this.name = name;
        this.host = host;
        this.port = port;
        this.type = type;
        this.active = active;
        this.username = username;
        this.password = password;

        List<Train> trains = new ArrayList<>();
        areas = new ArrayList<>();
    }

/*    public Server(String name, int port, boolean status, String about) {
        this.name = name;
        this.port = port;
        this.about = about;
        this.areas = new ArrayList<>();
        this.status = status;
        this.active = false;

        this.serverId = id.incrementAndGet();
    } */

    /*public Server(String attribute, String attribute1, String attribute2, String attribute3,
                  String attribute4, String attribute5) {
        this.name = attribute;
        this.ipAdr = attribute1;
        this.port = Integer.parseInt(attribute2);
        this.about = attribute3;
        fixAbout();
        this.userName = attribute4;
        this.userPassword = attribute5;
    }*/

    public String GetLoko(String token, String adr) {
        String text = null;
        //-:LOK;G;PLEASE;id_stanice;{poznámka}                 - zadost o rizeni konkretni lokomotivy; token neni potreba pripojovat v pripade, kdy loko uz mame autorizovane a bylo nam ukradeno napriklad mysi
        List<Train> trains;
        for (ControlArea a : areas) {
            trains = a.getTrains();
            for (Train t : trains) {
                if (t.getName().equals(adr)) {
                    if (t.isAuthorized()) {
                        token = t.getToken();
                    }
                }
            }
        }
        text = token != null ? "-:LOK;" + adr + ";PLEASE;" + token : "-:LOK;" + adr + ";PLEASE";
        return text;
    }

    public String Release(String adr) {
        String text = null;
        text = "-:LOK;" + adr + "RELEASE";
        return text;
    }

    public String getLongInfo() {
        return "Server :" + name + "\n" +
                "Ip: " + host + "\n" +
                "Port: " + port + "\n" +
                "Type: " + type + "\n" +
                "Active: " + ((TCPClientApplication.getInstance().server == this &&
                               TCPClientApplication.getInstance().connected()) ? "yes" : "no");
    }

    public Train getTrain(String adr) {
        List<Train> trains;
        for (ControlArea a : areas) {
            trains = a.getTrains();
            for (Train t : trains) {
                if (t.getUserLokoName().equals(adr) || t.getName().equals(adr)) return t;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Server --" + name + "--" +
                "| ip=" + host +
                "| port=" + port + ' ';
    }

    public ArrayList<String> getAuthorizedTrainsString() {
        ArrayList<String> temp = new ArrayList<>();
        List<Train> trains;
        if (areas.size() > 0) {
            for (ControlArea a : areas) {
                trains = a.getTrains();
                if ((trains.size() > 0)) {
                    for (Train s : trains) {
                        if (s.isAuthorized()) {
                            temp.add(s.nameString());
                        }
                    }
                }
            }
            if (!temp.isEmpty()) return temp;
        }
        return temp;
    }

    public ArrayList<String> getTrainString() {
        ArrayList<String> temp = new ArrayList<>();
        List<Train> trains;
        if (areas.size() > 0) {
            for (ControlArea a : areas) {
                trains = a.getTrains();
                if ((trains.size() > 0)) {

                    for (Train s : trains) {

                        if (s.isAuthorized()) {
                            temp.add(s.toString());
                        }
                    }
                }
            }
            if (!temp.isEmpty()) return temp;
        }
        temp.add("no trains loaded");
        return temp;
    }

    public void addArea(ControlArea area) {
        this.areas.add(area);
    }

    public ArrayList<String> getAreasId() {
        ArrayList<String> id = new ArrayList<>();
        for (ControlArea a : areas) {
            id.add(a.getId());
        }
        return id;
    }


    public ArrayList<String> getUnAuthorizedAreas() {
        ArrayList<String> temp = new ArrayList<>();
        if ((areas.size() > 0)) {
            for (ControlArea s : areas) {
                temp.add(s.getName());
            }
            if (!temp.isEmpty()) return temp;
        }
        temp.add("no areas loaded");
        return temp;
    }

    public Collection<? extends String> getAuthorizedAreas() {
        ArrayList<String> temp = new ArrayList<>();
        if ((areas.size() > 0)) {
            for (ControlArea s : areas) {
                temp.add("area:" + s.getName());
            }
            if (!temp.isEmpty()) return temp;
        }
        temp.add("no areas loaded");
        return temp;
    }

    public ControlArea getArea(String itemValue) {
        for (ControlArea c : areas) {
            if (c.getName().equals(itemValue)) return c;
        }
        return null;
    }

    public String getAreaServerString(String itemValue) {
        String tmp[] = itemValue.split(" ", 2);
        //Log.e("", "prvni: " + tmp[0] + " druha: " + tmp[1]);
        //ControlArea c = this.getArea(tmp[1]);
        ControlArea c = this.getArea(itemValue);
        Log.e("", "id: " + c.getId());
        String text = "-;LOK;G;PLEASE;" + c.getId() + ";";
        return text;
    }


    public String getStringData() {
        String statusText = this.active ? "online" : "offline";
        String stringData = this.name + "\t" + this.host + "\n" + this.type + " \t" + statusText;
        return stringData;
    }

    public String getStoredStringData() {
        String stringData = this.name + "\t" + this.host + "\n" + this.type;
        return stringData;
    }

    public String getSaveDataString() {
        return this.name + ";" + this.host + ";" + this.port + ";"
                + this.type + ";" + this.username + ";" + this.password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        if (port != server.port) return false;
        if (!name.equals(server.name)) return false;
        return host != null ? host.equals(server.host) : server.host == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }

    public void addTrain(Train newTrain) {
        //TODO dodelat
        ControlArea tmp = this.areas.get(areas.size() - 1);
        if (tmp.getTrains().contains(newTrain)) tmp.getTrains().remove(newTrain);
        tmp.addTrain(newTrain);
    }

   /* public ArrayList<Train> getTrains() {
        ArrayList<Train> trains = new ArrayList<>();
        trains.add(new Train("bnn21f86", false, new boolean[16], 16, true));
        trains.add(new Train("aaaa", true, new boolean[16], 16, false));
        trains.add(new Train("nov", true, new boolean[16], 16, true));
        trains.get(0).setAuthorized(true);
        trains.get(1).setAuthorized(true);
        trains.get(2).setAuthorized(true);
        return trains;
    }*/

    public void removeTrain(Train train) {
        List<Train> trains;
        for (ControlArea a : areas) {
            trains = a.getTrains();
            if (trains.contains(train))
                trains.remove(train);
        }
    }
}