package cz.mendelu.xmarik.train_manager.helpers;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashHelper allows for hashing into SHA-256.
 */
public class HashHelper {
    public static String hashPasswd(String passwd) {
        MessageDigest md;
        byte[] byteData = {};
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(passwd.getBytes());
            byteData = md.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e("HashHelper", "NoSuchAlgorithmException", e);
        }
        //convert the byte to hex format method 1
        StringBuilder sb = new StringBuilder();
        for (byte aByteData : byteData) {
            sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

}
