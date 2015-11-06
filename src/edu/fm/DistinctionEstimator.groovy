package edu.fm

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam

/**
 * User: artem.smirnov
 * Date: 06.11.2015
 * Time: 10:41
 */
class DistinctionEstimator {

    public static <T> T getSimilar(Collection<T> els, String baseSongName, @ClosureParams(FirstParam.FirstGenericType.class) Closure<String> c) {
        def mostSimilar = getMostSimilar(els, baseSongName, c)

        def foundName = c.call(mostSimilar)

        if (diffFactor(baseSongName, foundName) > 10) {
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
