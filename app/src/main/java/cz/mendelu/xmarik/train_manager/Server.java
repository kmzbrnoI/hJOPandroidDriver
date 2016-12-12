package cz.mendelu.xmarik.train_manager;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ja on 15. 6. 2016.
 */
public class Server {
    public static AtomicInteger id = new AtomicInteger(0);
    public int serverId;
    public String name;
    public String ipAdr;
    public int port;
    public String about;
    public boolean status;
    public List<ControlArea> areas;
    private String base = "-;LOK;G;";
    private boolean active;
    private String dnsName;
    private String userName;
    private String userPassword;
    private TCPClient tcpClient;

//verze_protokolu;typ_zarizeni;server_nazev;server_ip;server_port;
    public Server(String name, String ipAdr, int port, boolean status, String about) {
        this.ipAdr = ipAdr;
        this.name = name;
        this.port = port;
        this.about = about;
        this.areas = new ArrayList<>();
        this.status = status;
        this.active = false;
        this.serverId = id.incrementAndGet();
        List<Train> trains = new ArrayList<>();
        trains.add(new Train("bnn21f86", false, new boolean[16], 16, true, "loko1", 10));
        trains.add(new Train("aaaa", true, new boolean[16], 16, false, "loko2", 15));
        trains.add(new Train("nov", true, new boolean[16], 16, true, "loko3", 100));
        trains.get(0).setAuthorized(true);
        trains.get(2).setAuthorized(true);
        areas.add(new ControlArea("1", "full", trains));
        areas.add(new ControlArea("1", "empty", new ArrayList<Train>()));
    }

    public Server(String name, int port, boolean status, String about) {
        this.name = name;
        this.port = port;
        this.about = about;
        this.areas = new ArrayList<>();
        this.status = status;
        this.active = false;

        this.serverId = id.incrementAndGet();
    }

    public Server(String attribute, String attribute1, String attribute2, String attribute3,
                  String attribute4, String attribute5) {
        this.name = attribute;
        this.ipAdr = attribute1;
        this.port = Integer.parseInt(attribute2);
        this.about = attribute3;
        this.userName = attribute4;
        this.userPassword = attribute5;
    }

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
        return "Server :" + about + "\n" +
                "ip = " + ipAdr + "\n" +
                "port = " + port + "\n" +
                "typ = " + name + "\n" +
                "aktivní = " + (active ? "ano" : "ne");
    }

    public String getBase() {
        return base;
    }

    public boolean isActive() {
        return active;
    }

    public String getDnsName() {
        return dnsName;
    }

    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean ac) {

        this.active = ac;

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
                "| ip=" + ipAdr +
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
                temp.add(s.getId() + " " + s.getName());
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
        Log.e("", "prvni: " + tmp[0] + " druha: " + tmp[1]);
        ControlArea c = this.getArea(tmp[1]);
        Log.e("", "id: " + c.getId());
        String text = "-;LOK;G;PLEASE;" + c.getId() + ";";
        return text;
    }


    public String getStringData() {
        String statusText = this.status ? "online" : "offline";
        String stringData = this.name + "\t" + this.ipAdr + "\n" + this.about + " \t" + statusText;
        return stringData;
    }

    public String getSaveDataString() {
        return this.name + ";" + this.ipAdr + ";" + this.port + ";"
                + this.about + ";" + this.userName + ";" + this.userPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        if (port != server.port) return false;
        if (!name.equals(server.name)) return false;
        return ipAdr != null ? ipAdr.equals(server.ipAdr) : server.ipAdr == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (ipAdr != null ? ipAdr.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }

    public void setTcpClient(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public String getInfo() {
        return this.about +" uživatel:"+ this.userName;
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

    public String getStoredStringData() {
        String stringData = this.name + "\t " + this.ipAdr+ " " + this.about;
        return stringData;
    }
}