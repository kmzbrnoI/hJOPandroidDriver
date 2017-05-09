package cz.mendelu.xmarik.train_manager;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.mendelu.xmarik.train_manager.activities.ServerSelect;
import cz.mendelu.xmarik.train_manager.events.ReloadEvent;
import cz.mendelu.xmarik.train_manager.models.Server;


/**
 * Created by ja on 15. 3. 2016.
 */
//URL, Integer, Long
public class UdpDiscover extends AsyncTask<String, Void, String> {
    private static final int TIMEOUT_MS = 500;
    InetAddress nov = null;
    String message;
    int DISCOVERY_PORT;
    ServerList reciver;
    ServerSelect main;
    private WifiManager mWifi;


    public UdpDiscover(Context context, int port, ServerSelect mainActivity) {
        mWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String zprava = "hJOP;1.0;regulator;mobileManager;";
        message = zprava + this.getIPAddress(true) + ";" + "\n";
        DISCOVERY_PORT = port;
        reciver = ServerList.getInstance();
        main = mainActivity;
    }

    public void run() {

    }

    /**
     * Send a broadcast UDP packet containing a request for train server services to
     * announce themselves.
     *
     * @throws IOException
     */
    private void sendDiscoveryRequest(DatagramSocket socket) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message
                .length(), getBroadcastAddress(), DISCOVERY_PORT);
        socket.send(packet);
    }

    /**
     * Calculate the broadcast IP we need to send the packet along. If we send
     * it to 255.255.255.255, it never gets sent. I guess this has something to
     * do with the mobile network not wanting to do broadcast.
     */
    public InetAddress getBroadcastAddress() throws IOException {
        DhcpInfo dhcp = mWifi.getDhcpInfo();
        if (dhcp == null) {
            //"Could not get dhcp info"
            return null;
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);

        return InetAddress.getByAddress(quads);
    }

    /**
     * Listen on socket for responses, timing out after TIMEOUT_MS
     *
     * @param socket socket on which the announcement request was sent
     * @return list of discovered servers, never null
     * @throws IOException
     */
    private ArrayList<Server> listenForResponses(DatagramSocket socket)
            throws IOException {
        long start = System.currentTimeMillis();
        byte[] buf = new byte[1024];
        ArrayList<Server> servers = new ArrayList<>();

        // Loop and try to receive responses until the timeout elapses. We'll
        // get
        // back the packet we just sent out, which isn't terribly helpful, but
        // we'll
        // discard it in parseResponse because the cmd is wrong.
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String s = new String(packet.getData(), 0, packet.getLength());

                Server server = parseServerMessage(s);
                if (server != null)
                    servers.add(server);
            }
        } catch (SocketTimeoutException e) {
            Log.e("listening exception", "S: time out '" + e + "'");
        }
        return servers;
    }

    public InetAddress zjisti(int port) {
        String zprava = "hJOP;1.0;panel;;";
        zprava = zprava + this.getIPAddress(true) + ";";

        return nov;
    }

    private Server parseServerMessage(String message) {
        //"hJOP";verze_protokolu;typ_zarizeni;server_nazev;server_ip;server_port;
        //server_status;server_popis
        String[] tmp = HelpServices.parseHelper(message);
        Server server = null;

        if ((tmp.length > 0) && (tmp[0].equals("hJOP"))) {
            if (tmp[2].equals("server")) {
                server = new Server(tmp.length > 4 ? tmp[7] : "",
                        tmp.length > 5 ? tmp[4] : "",
                        tmp.length > 6 ? Integer.parseInt(tmp[5]) : 0,
                        tmp.length > 7 ? tmp[6].equals("on") : null,
                        tmp.length >= 8 ? tmp[3] : "");
            }
        }
        return server;
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    private String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    @Override
    protected String doInBackground(String... urls) {
        ArrayList<Server> servers = null;
        try {
            DatagramSocket sock = new DatagramSocket(null);
            sock.setReuseAddress(true);
            sock.bind(new InetSocketAddress(DISCOVERY_PORT));
            DatagramSocket socket = sock;
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_MS);
            sendDiscoveryRequest(socket);
            servers = listenForResponses(socket);
            socket.close();
        } catch (Exception e) {
            servers = new ArrayList<Server>(); // use an empty one
            Log.e("exception", "S: Received Message: '" + e + "'");
            //"Could not send discovery request", e);
        }
        reciver.addServer(servers);

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        EventBus.getDefault().post(new ReloadEvent(""));
    }


}
