package edu.fm

import groovy.util.logging.Slf4j

/**
 * User: artem.smirnov
 * Date: 10.11.2015
 * Time: 17:14
 */
@Slf4j
class FindMuzicRunner {

    String argPath

    String loadListFilename
    String failedListFilename
    String succeededListFilename
    String mappingListFilename
    String resultsSubDir

    Integer songsOffset
    Integer songsCount

    def run() {

        File workDir = FileTools.getDir(argPath)
        File resultFolder = FileTools.getSubDir(workDir, resultsSubDir)

        List<String> songNames = FileTools.readSongs(workDir, loadListFilename)
        List<String> loadedSongs = FileTools.getLoadedSongs(resultFolder)

        List<String> failedSongNames = FileTools.readSongs(workDir, failedListFilename)
        List<String> succeededSongNames = FileTools.readSongs(workDir, succeededListFilename)

        List<String> subList = getSubList(songNames, songsOffset, songsCount)
        List<String> songsToLoad = subList
        songsToLoad = filterAlreadyLoaded(songsToLoad, loadedSongs)
        songsToLoad = filterSongs(songsToLoad, failedSongNames)
        songsToLoad = filterSongs(songsToLoad, succeededSongNames)

        if (subList.size() != songsToLoad.size()) {
            log.warn("${subList.size() - songsToLoad.size()}/${subList.size()} already exist and would not be loaded")
        }

//        int counter = songsOffset
        int failureCounter = 0


        def failed = new ArrayList<String>()
        def succeeded = new ArrayList<String>()

        def mapping = new HashMap<String, String>()

        try {
            songsToLoad.each {

                log.info("searching for '${it}'... ")
                try {
                    def song = Site7bxRuProvider.fetchSong(it)
                    DownloadTools.downloadFile(song.foundSongName, song.downloadUrl, resultFolder)

                    mapping.put(it, song.foundSongName)
                    succeeded.add(it)
                    log.info("succeeded")
                    failureCounter = 0
                    sleep(1000)
                } catch (Throwable e) {
                    failed.add(it)
                    log.error("${e.message}...")
                    log.error("failed")
                    failureCounter++
                    if (failureCounter > 40) {
                        failed.addAll(songsToLoad.subList(songsToLoad.indexOf(it), songsToLoad.size()))
                        throw e
                    }
                }

            }
        } finally {
            log.info("")
            log.info("SUMMARY ")
            log.info("SUCCEEDED (${succeeded.size()}): ")
            succeeded.each { s ->
                log.info(s)
            }
            log.info("FAILED (${failed.size()}): ")
            failed.each { f ->
                log.info(f)
            }
        }

        FileTools.writeSongs(workDir, failed, failedListFilename, false)
        FileTools.writeSongs(workDir, succeeded, succeededListFilename, false)
        FileTools.writeMapping(workDir, mapping, mappingListFilename, false)
    }

    private ArrayList<String> filterSongs(List<String> subList, List<String> songs) {
        List<String> list = new ArrayList<>()
        for (String songName : subList) {
            if (!songs.contains(songName)) {
                list.add(songName)
            }
        }
        list
    }

    private ArrayList<String> filterAlreadyLoaded(List<String> subList, List<String> loadedSongs) {
        List<String> list = new ArrayList<>()
        for (String songName : subList) {
            if (!DistinctionEstimator.containsExact(loadedSongs, songName, { it })) {
                list.add(songName)
            }
        }
        list
    }

    private List<String> getSubList(Collection<String> songNames, int songsOffset, int songsCount) {
        List<String> songsToLoad
        if (songsOffset != null) {
            def count = (songsCount != null) ? songsCount : songNames.size() - songsOffset
            songsToLoad = new ArrayList<String>(songNames).subList(songsOffset, songsOffset + count)
        } else {
            songsToLoad = new ArrayList<String>(songNames)
        }
        songsToLoad
    }
}
