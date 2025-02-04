package cz.mendelu.xmarik.train_manager.models;

import java.util.ArrayList;
import java.util.Objects;

import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;

/**
 * Class Server represents a single server. Server contains database of all trains.
 */
public class Server {
    public String name;
    public String host;
    public int port;
    public String type;
    public boolean active;

    public String username;
    public String password;

    public Server(String name, String host, int port, boolean active, String type, String username, String password) {
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

        this.name = parsed.get(7);
        this.host = parsed.get(4);
        this.port = Integer.parseInt(parsed.get(5));
        this.type = parsed.get(3);
        this.active = parsed.get(6).equals("on");
        this.username = "";
        this.password = "";
    }

    public String getTitle() {
        return this.name.isEmpty() ? this.host : this.name + "\u00A0\u00A0" + this.host;
    }

    public String getSaveDataString() {
        return this.name + ";" + this.host + ";" + this.port + ";"
                + this.type + ";" + this.username + ";" + this.password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server other = (Server)o;
        return (this.name.equals(other.name)) && (this.port == other.port) && (this.host.equals(other.host));
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ (host != null ? host.hashCode() : 0) ^ port;
    }
}
