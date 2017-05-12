package cz.mendelu.xmarik.train_manager.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parse Helper allowes parsing data from hJOPserver in format of semicolon-separated-values.
 */
public class ParseHelper {
    public static ArrayList<String> parse(String text, String separators, String ignore) {
        ArrayList<String> result = new ArrayList<>();
        String s = "";
        int plain_cnt = 0;
        if (text == "") return new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '{') {
                if (plain_cnt > 0) s = s + text.charAt(i);
                plain_cnt++;
            } else if ((text.charAt(i) == '}') && (plain_cnt > 0)) {
                plain_cnt--;
                if (plain_cnt > 0) s = s + text.charAt(i);
            } else if (separators.indexOf(text.charAt(i)) != -1 && plain_cnt == 0) {
                result.add(s);
                s = "";
            } else if (ignore.indexOf(text.charAt(i)) == -1 || plain_cnt > 0) {
                s = s + text.charAt(i);
            }
        }

        if (s != "") result.add(s);
        return result;
    }
}
