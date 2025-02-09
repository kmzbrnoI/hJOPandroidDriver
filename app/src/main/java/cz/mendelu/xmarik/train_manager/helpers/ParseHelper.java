package cz.mendelu.xmarik.train_manager.helpers;

import java.util.ArrayList;

/**
 * Parse Helper allowes parsing data from hJOPserver in format of semicolon-separated-values.
 */
public class ParseHelper {
    public static ArrayList<String> parse(String text, String separators, String ignore) {
        ArrayList<String> result = new ArrayList<>();
        StringBuilder s = new StringBuilder();
        int plain_cnt = 0;
        if (text.isEmpty())
            return new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '{') {
                if (plain_cnt > 0) s.append(text.charAt(i));
                plain_cnt++;
            } else if ((text.charAt(i) == '}') && (plain_cnt > 0)) {
                plain_cnt--;
                if (plain_cnt > 0) s.append(text.charAt(i));
            } else if (separators.indexOf(text.charAt(i)) != -1 && plain_cnt == 0) {
                result.add(s.toString());
                s = new StringBuilder();
            } else if (ignore.indexOf(text.charAt(i)) == -1 || plain_cnt > 0) {
                s.append(text.charAt(i));
            }
        }

        if (!s.toString().isEmpty())
            result.add(s.toString());
        return result;
    }
}
