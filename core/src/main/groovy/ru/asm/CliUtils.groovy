package ru.asm

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * User: artem.smirnov
 * Date: 17.06.2016
 * Time: 9:27
 */
class CliUtils {

    public static String[] split(String str) {
        final ArrayList<String> result = new ArrayList<>();

        str = str + " "; // add trailing space
        int len = str.length();
        Matcher m = Pattern.compile("((\"[^\"]+?\")|('[^']+?')|([^\\s]+?))\\s++").matcher(str);

        for (int i = 0; i < len; i++) {
            m.region(i, len);

            if (m.lookingAt()) {
                String s = m.group(1);

                if ((s.startsWith("\"") && s.endsWith("\"")) ||
                        (s.startsWith("'") && s.endsWith("'"))) {
                    s = s.substring(1, s.length() - 1);
                }

                i += (m.group(0).length() - 1);
                result.add(s);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    static String[] args(String[] parts) {
     def params = Arrays.asList(parts).subList(1, parts.size())
     def array = params.toArray(new String[params.size()])
     array
 }

    static String command(String[] parts) {
     def command = parts[0]
     command
 }

    static String arg(String[] args, Integer index) {
     args.size() > index ? args[index] : null
 }
}
