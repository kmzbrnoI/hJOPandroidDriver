package cz.mendelu.xmarik.train_manager.models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
