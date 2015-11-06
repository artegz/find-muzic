package edu.fm

import org.apache.commons.io.IOCase
import org.apache.commons.io.filefilter.SuffixFileFilter

/**
 * User: artem.smirnov
 * Date: 06.11.2015
 * Time: 11:00
 */
class FileTools {

    static File getDir(String dirName) {
        def dir = new File(dirName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    static List<String> getLoadedSongs(File workDir) {
        def result = new ArrayList<String>()
        def filter = new SuffixFileFilter("mp3", IOCase.INSENSITIVE)
        for (File mp3File : workDir.listFiles((FileFilter) filter)) {
            result.add(mp3File.getName().replace(".mp3", ""))
        }
        result
    }

    static List<String> readSongs(File workDir, String filename) {
        def res = new TreeSet<String>()
        def songsFile = new File(workDir, filename)
        if (songsFile.exists()) {
            songsFile.withReader { r ->
                r.eachLine {
                    res.add(it)
                }
            }
        } else {
            println songsFile.getName() + "doesn't exsist"
        }
        new ArrayList<>(res)
    }

    static void writeMapping(File workDir, Map<String, String> songs, String filename, boolean override) {
        def songsFile = new File(workDir, filename)
        if (override) {
            if (songsFile.exists()) {
                songsFile.delete()
            }
            songsFile.createNewFile()
        } else if (!songsFile.exists()) {
            songsFile.createNewFile();
        }

        if (override) {
            songsFile.withWriter { out ->
                songs.each {
                    out.println it.key + "\t" + it.value
                }
            }
        } else {
            songsFile.withWriterAppend { out ->
                songs.each {
                    out.println it.key + "\t" + it.value
                }
            }
        }

    }

    static void writeSongs(File workDir, Collection<String> songs, String filename, boolean override) {
        def songsFile = new File(workDir, filename)
        if (override) {
            if (songsFile.exists()) {
                songsFile.delete()
            }
            songsFile.createNewFile()
        } else if (!songsFile.exists()) {
            songsFile.createNewFile();
        }

        if (override) {
            songsFile.withWriter { out ->
                songs.each {
                    out.println it
                }
            }
        } else {
            songsFile.withWriterAppend { out ->
                songs.each {
                    out.println it
                }
            }
        }
    }

    static File getSubDir(File workDir, String subDirName) {
     File resultFolder = new File(workDir, subDirName)
     if (!resultFolder.exists()) resultFolder.mkdirs()
     resultFolder
 }
}
