package edu.fm

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam

/**
 * User: artem.smirnov
 * Date: 06.11.2015
 * Time: 10:41
 */
class DistinctionEstimator {

    private static char[] alphabet = "abcdefghijklmnopqrstuvwxyzàáâãäå¸æçèéêëìíîïðñòóôõö÷øùúüýþÿ1234567890".toCharArray()

    static Integer maxDiffFactor = 10;

    public static <T> boolean containsExact(Collection<T> els, String baseSongName, @ClosureParams(FirstParam.FirstGenericType.class) Closure<String> c) {
        boolean contains;
        try {
            if (getExact(els, baseSongName, c) != null) {
                contains = true;
            } else {
                contains = false;
            }
        } catch (Throwable e) {
            contains = false;
        }
        contains
    }

    public static <T> T getSimilar(Collection<T> els, String baseSongName, @ClosureParams(FirstParam.FirstGenericType.class) Closure<String> c) {
        def mostSimilar = getMostSimilar(els, baseSongName, c)

        def foundName = c.call(mostSimilar)

        if (maxDiffFactor != null && diffFactor(baseSongName, foundName) > maxDiffFactor) {
            throw new Exception("song '${baseSongName}' not found, most simular has name '${foundName}'");
        }

        mostSimilar
    }

    public static <T> T getExact(Collection<T> els, String baseSongName, @ClosureParams(FirstParam.FirstGenericType.class) Closure<String> c) {
        def mostSimilar = getMostSimilar(els, baseSongName, c)

        def foundName = c.call(mostSimilar)

        if (diffFactor(baseSongName, foundName) > 0) {
            throw new Exception("song '${baseSongName}' not found, most simular has name '${foundName}'");
        }

        mostSimilar
    }

    private static <T> T getMostSimilar(Collection<T> els, String baseSongName, @ClosureParams(FirstParam.FirstGenericType.class) Closure<String> c) {
        def sorted = new ArrayList<T>(els)
        sorted.sort { v1, v2 ->
            diffFactor(baseSongName, c.call(v1)) - diffFactor(baseSongName, c.call(v2))
        }

        if (sorted.isEmpty()) {
            throw new Exception("nothing found")
        }

        sorted.get(0)
    }

    private static Integer diffFactor(String keyI, String valueI) {
        def key = convert(keyI)
        def value = convert(valueI)

        int difSize = 0

        for (char c : alphabet) {
            char[] ar = [ c ]
            def str = new String(ar)
            difSize += Math.abs(key.count(str) - value.count(str))
        }

        difSize
    }

    private static Integer diffFactor2(String keyI, String valueI) {
        def key = convert(keyI)
        def value = convert(valueI)

        Integer difSize = 0
        if (value.length() > key.length()) {
            difSize += value.length() - key.length()
        }

        int maxI = [key.length(), value.length()].min()

        value.chars.eachWithIndex { c, i ->
            if (i < maxI)
                if (c != key.chars[i]) {
                    difSize++
                }
        }
        if (key.length() > value.length()) {
            difSize += key.length() + value.length()
        }
        difSize
    }

    private static String convert(String testStr) {
        testStr.toLowerCase().replaceAll("[\\p{Punct}\\s]*", "")
    }
}
