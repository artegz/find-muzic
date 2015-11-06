package edu.fm

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

        songsFile.withWriter { out ->
            songs.each {
                out.println it
            }
        }
    }

    static File getSubDir(File workDir, String subDirName) {
     File resultFolder = new File(workDir, subDirName)
     if (!resultFolder.exists()) resultFolder.mkdirs()
     resultFolder
 }
}
