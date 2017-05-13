package cz.mendelu.xmarik.train_manager.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashHelper allows for hashing into SHA-256.
 */
public class HashHelper {
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
