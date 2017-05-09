package cz.mendelu.xmarik.train_manager;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by ja on 1. 7. 2016.
 */
public class HelpServices {

    public static String[] parseHelper(String s) {
        Log.v("Parse", s);
        String splitByBrecket[] = s.split("\\{.*\\}");
        ArrayList<String> response = new ArrayList<>();
        for (String tmp : splitByBrecket) {
            Collections.addAll(response, tmp.split(";"));
        }

        return response.toArray(new String[0]);
    }

    public static String[] trainParseHelper(String s) {
        int count = 0;
        int start = 0;
        int end = 0, lastEnd = 0;
        ArrayList<String> tmp = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '{') {
                if (count == 0) {
                    start = i;
                    count++;
                } else count++;
            } else if (s.charAt(i) == '}') {
                if (count == 1) {
                    end = i;
                    if (start > 0) {
                        String toParse = s.substring(lastEnd, start - 1);
                        String[] field = toParse.split(";");
                        Collections.addAll(tmp, field);
                        tmp.add(s.substring(start + 1, end - 1));
                        lastEnd = end + 1;
                    } else {
                        tmp.add(s.substring(start + 1, end - 1));
                        lastEnd = end + 1;
                    }
                }
                count--;
            }
        }
        if (lastEnd + 1 < s.length()) {
            String toParse = s.substring(lastEnd, s.length() - 1);
            String[] field = toParse.split(";");
            Collections.addAll(tmp, field);
        }

        return tmp.toArray(new String[0]);
    }

    public static String domainName(String ipAddr) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ipAddr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "Unknown";
        }
        String host = addr.getHostName();
        System.out.println(host);
        return host;
    }

    public static String ipAddressFromDomainName() {
        InetAddress address = null;
        try {
            address = InetAddress.getByName("www.example.com");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            //TODO nejak to dopracovat
        }

        return address != null ? address.getHostAddress() : null;
    }

    public static String hashPasswd(String passwd) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(passwd.getBytes());
        byte byteData[] = md.digest();
        //convert the byte to hex format method 1
        StringBuilder sb = new StringBuilder();
        for (byte aByteData : byteData) {
            sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

}
