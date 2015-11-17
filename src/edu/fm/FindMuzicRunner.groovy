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

    boolean excludeFailed = false
    boolean excludeSucceeded = false
    boolean excludeAlreadyExist = true

    def run() {

        File workDir = FileTools.getDir(argPath)
        File resultFolder = FileTools.getSubDir(workDir, resultsSubDir)

        List<String> songNames = FileTools.readSongs(workDir, loadListFilename)
        List<String> loadedSongs = FileTools.getLoadedSongs(resultFolder)

        List<String> failedSongNames = FileTools.readSongs(workDir, failedListFilename)
        List<String> succeededSongNames = FileTools.readSongs(workDir, succeededListFilename)

        List<String> subList = getSubList(songNames, songsOffset, songsCount)
        Map<String, Boolean> songs = asMap(subList)
        if (excludeAlreadyExist) {
            filterAlreadyLoaded(songs, loadedSongs)
        }
        if (excludeFailed) {
            filterSongs(songs, failedSongNames)
        }
        if (excludeSucceeded) {
            filterSongs(songs, succeededSongNames)
        }

//        if (subList.size() != songs.size()) {
//            log.warn("${subList.size() - songs.size()}/${subList.size()} already exist and would not be loaded")
//        }

        int counter = songsOffset
        int failureCounter = 0


        def failed = new ArrayList<String>()
        def succeeded = new ArrayList<String>()

        def mapping = new HashMap<String, String>()

        try {
            songs.each {
                def songName = it.key

                if (it.value) {
                    log.info("[${counter++}] searching for '${songName}'... ")
                    try {
                        def song = Site7bxRuDownloadProvider.fetchSong(songName)
                        DownloadTools.downloadFile(song.foundSongName, song.downloadUrl, resultFolder)

                        mapping.put(songName, song.foundSongName)
                        succeeded.add(songName)
                        log.info("succeeded")
                        failureCounter = 0
                        sleep(1000)
                    } catch (Throwable e) {
                        failed.add(songName)
                        log.error("${e.message}...")
                        log.error("failed")
                        failureCounter++
                        if (failureCounter > 40) {
                            def songNamesAsList = new ArrayList<>(songs.keySet())
                            failed.addAll(songNamesAsList.subList(songNamesAsList.indexOf(songName), songNamesAsList.size()))
                            throw e
                        }
                    }
                } else {
                    log.warn("[${counter++}] '${songName}' skipped")
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

    private Map<String, Boolean> asMap(List<String> subList) {
        Map<String, Boolean> songsToLoad = new TreeMap<>()
        for (String song : subList) {
            songsToLoad.put(song, true)
        }
        songsToLoad
    }

    private void filterSongs(Map<String, Boolean> songs, List<String> songsToFilter) {
        List<String> filteredSongs = new ArrayList<>()
        for (String songName : songs.keySet()) {
            if (songsToFilter.contains(songName)) {
                filteredSongs.add(songName)
            }
        }
        for (String filteredSong : filteredSongs) {
            songs.put(filteredSong, false)
        }
    }

    private void filterAlreadyLoaded(Map<String, Boolean> songs, List<String> loadedSongs) {
        def List<String> filteredSongs  = new ArrayList<>()
        for (String songName : songs.keySet()) {
            if (DistinctionEstimator.containsExact(loadedSongs, songName, { it })) {
                filteredSongs.add(songName)
            }
        }
        for (String filteredSong : filteredSongs) {
            songs.put(filteredSong, false)
        }
    }

    private List<String> getSubList(Collection<String> songNames, int songsOffset, int songsCount) {
        List<String> songsToLoad
        if (songsOffset != null) {
            def count = (songsCount != null) ? songsCount : songNames.size() - songsOffset
            def end = Math.min(songsOffset + count, songNames.size())
            songsToLoad = new ArrayList<String>(songNames).subList(songsOffset, end)
        } else {
            songsToLoad = new ArrayList<String>(songNames)
        }
        songsToLoad
    }
}
