package cz.mendelu.xmarik.train_manager.models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;
import cz.mendelu.xmarik.train_manager.network.TCPClientApplication;

/**
 * Class Server represents a single server. Server contains database of all trains.
 */
public class Server {
    public static AtomicInteger globalId = new AtomicInteger(0);
    public static final int DEFAULT_PORT = 5823;

    public int id; // TODO necessarry ?
    public String name;
    public String host;
    public int port;
    public String type;
    public boolean active;

    public String username;
    public String password;

    public Server(String name, String host, int port, boolean active, String type, String username, String password) {
        this.id = globalId.incrementAndGet();
        this.name = name;
        this.host = host;
        this.port = port;
        this.type = type;
        this.active = active;
        this.username = username;
        this.password = password;
    }

    // Create server from discovery packet.
    // @param disovery in format:
    //   "hJOP";verze_protokolu;typ_zarizeni;server_nazev;server_ip;server_port;server_status;server_popis
    public Server(String discovery) {
        ArrayList<String> parsed = ParseHelper.parse(discovery, ";", "");

        this.id = globalId.incrementAndGet();
        this.name = parsed.get(7);
        this.host = parsed.get(4);
        this.port = Integer.valueOf(parsed.get(5));
        this.type = parsed.get(3);
        this.active = parsed.get(6).equals("on");
        this.username = "";
        this.password = "";
    }

    public String getLongInfo() {
        return "Server :" + name + "\n" +
                "Ip: " + host + "\n" +
                "Port: " + port + "\n" +
                "Type: " + type + "\n" +
                "Active: " + ((TCPClientApplication.getInstance().server == this &&
                TCPClientApplication.getInstance().connected()) ? "yes" : "no");
    }

    @Override
    public String toString() {
        return "Server --" + name + "--" +
                "| ip=" + host +
                "| port=" + port + ' ';
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
}
