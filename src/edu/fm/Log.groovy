package edu.fm
import groovy.util.logging.Slf4j
/**
 * User: artem.smirnov
 * Date: 10.11.2015
 * Time: 16:53
 */
@Slf4j
class Log {
//
//    static Logger logger = Logger.getLogger("")
//
//            static {
//                logger.setLevel(Level.INFO)
//            }

    public static void log(GString str) {
        log.info(str.toString())
        //print(str)
    }

    public static void log(String str) {
        log.info(str)
        //print(str)
    }

    public static void logln(String str) {
        log.info(str)
        //println(str)
    }

    public static void logln(GString str) {
        log.info(str.toString())
        //println(str)
    }

    }
