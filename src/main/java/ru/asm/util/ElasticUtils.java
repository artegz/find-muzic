package ru.asm.util;

import java.util.ArrayList;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 31.03.2017
 * Time: 12:11
 */
public class ElasticUtils {

    public static String[] asTerms(String artist) {
        final String lowerCase = artist.toLowerCase();

        final String[] parts = lowerCase.replace("/", " ")
                .replace("'", " ")
                .replace("&", " ")
                .replace("*", "?")
                .replace(".", "")
                .replace("-", " ")
                .replace("!", " ")
                .replace("_", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replace(",", " ")
                .split(" ");

        final ArrayList<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                final String trimPart = part.trim();
                if (trimPart.equals("и")
                        || trimPart.equals("feat")) {
                    continue;
                }

                terms.add(trimPart);
            }
        }

        return terms.toArray(new String[terms.size()]);
    }

    public static String[] asTerms2(String artist) {
        final String lowerCase = artist.toLowerCase();

        final String[] parts = lowerCase.replace("/", " ")
                .replace("'", " ")
                .replace("&", " ")
                .replace("*", "?")
                .replace(".", "")
                .replace("-", " ")
                .replace("!", " ")
                .replace("_", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replace(",", " ")
                .split(" ");

        final ArrayList<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                final String trimPart = part.trim();
                if (trimPart.equals("и")
                        || trimPart.equals("feat")) {
                    continue;
                }

                terms.add(trimPart);
            }
        }

        return terms.toArray(new String[terms.size()]);
    }

    public static List<String> toLowerAll(String[] origVals) {
        ArrayList<String> result = new ArrayList<String>();
        for (String origVal : origVals) {
            result.add(origVal.toLowerCase());
//            result.add(origVal)
        }
        return result;
    }

    public static String[] asTerms3(String artist) {
        final String lowerCase = artist.toLowerCase();

        final String[] parts = lowerCase.replace("/", " ")
                .replace("'", " ")
                .replace("&", " ")
                .replace("*", "?")
                .replace(".", "")
                .replace("-", " ")
                .replace("!", " ")
                .replace("_", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replace(",", " ")
                .split(" ");

        final ArrayList<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                final String trimPart = part.trim();
                if (trimPart.equals("и")
                        || trimPart.equals("feat")) {
                    continue;
                }

                terms.add(trimPart);
            }
        }

        return terms.toArray(new String[terms.size()]);
    }
}
