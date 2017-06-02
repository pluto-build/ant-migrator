package antplutomigrator.utils;

/**
 * Created by manuel on 30.11.16.
 */
public class StringUtils {
    public static String decapitalize(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }
        char c[] = string.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }

    public static String capitalize(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }
        char c[] = string.toCharArray();
        c[0] = Character.toUpperCase(c[0]);
        return new String(c);
    }

    public static String javaPrint(String string) {
        return "\""+string.replace("\n","\\n")+"\"";
    }
}
