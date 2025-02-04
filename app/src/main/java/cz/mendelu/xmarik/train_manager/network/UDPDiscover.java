package cz.mendelu.xmarik.train_manager.network;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
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

import cz.mendelu.xmarik.train_manager.events.UDPDiscoveryFinishedEvent;
import cz.mendelu.xmarik.train_manager.events.UDPNewServerEvent;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;
import cz.mendelu.xmarik.train_manager.models.Server;


/**
 * UDPDiscover discovers hJOPservers in local network.
 */
public class UDPDiscover extends Thread {
    public static final int DEFAULT_PORT = 5880;
    private static final int TIMEOUT_MS = 800;

    final WifiManager mWifi;

    public UDPDiscover(WifiManager mWifi) {
        this.mWifi = mWifi;
    }

    /**
     * Calculate the broadcast IP we need to send the packet along. If we send
     * it to 255.255.255.255, it never gets sent. I guess this has something to
     * do with the mobile network not wanting to do broadcast.
     */
    InetAddress getBroadcastAddress() throws IOException {
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
     * @throws IOException
     */
    private void listenForResponses(DatagramSocket socket)
            throws IOException {
        byte[] buf = new byte[1024];

        // Loop and try to receive responses until the timeout elapses.
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String s = new String(packet.getData(), 0, packet.getLength());

                Server server = parseServerMessage(s);
                if (server != null)
                    EventBus.getDefault().post(new UDPNewServerEvent(server));
            }
        } catch (SocketTimeoutException e) {
            // OK
        }
    }

    private Server parseServerMessage(String message) {
        //"hJOP";verze_protokolu;typ_zarizeni;server_nazev;server_ip;server_port;
        //server_status;server_popis
        ArrayList<String> parsed = ParseHelper.parse(message, ";", "");

        if (parsed.size() >= 8 && parsed.get(0).equals("hJOP") && parsed.get(2).equals("server"))
            return new Server(message);
        else
            return null;
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
        } catch (Exception ignored) {
        } // for now eat exceptions
        return "";
    }

    public void run() {
        try {
            DatagramSocket sock = new DatagramSocket(null);
            sock.setReuseAddress(true);
            sock.bind(new InetSocketAddress(DEFAULT_PORT));

            sock.setBroadcast(true);
            sock.setSoTimeout(TIMEOUT_MS);

            String message = "hJOP;1.0;regulator;mobileManager;" + this.getIPAddress(true) + ";\n";
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(),
                    getBroadcastAddress(), DEFAULT_PORT);
            sock.send(packet);

            listenForResponses(sock);
            sock.close();
        } catch (Exception e) {
            Log.e("exception", "S: Received Message: '" + e + "'");
        }

        EventBus.getDefault().post(new UDPDiscoveryFinishedEvent());
    }
}
