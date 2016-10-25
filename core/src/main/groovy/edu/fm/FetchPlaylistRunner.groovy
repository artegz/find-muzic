package edu.fm

import groovy.util.logging.Slf4j

import java.text.SimpleDateFormat

/**
 * User: artem.smirnov
 * Date: 19.11.2015
 * Time: 10:52
 */
@Slf4j
class FetchPlaylistRunner {

    String argDateFrom
    String argDateTo
    String argPath

    String station

    String outputFilename

    def run() {
        def dateFrom = new SimpleDateFormat("dd.MM.yyyy").parse(argDateFrom)
        def dateTo = new SimpleDateFormat("dd.MM.yyyy").parse(argDateTo)

        def workDir = FileTools.getDir(argPath)

        //def fetchedSongs = new SiteMoreradioRuPlaylistFetch().fetchPlaylist(dateFrom, dateTo, station)
        def fetchedSongs = Context.get().playlistProvider.fetchPlaylist(dateFrom, dateTo, station)

        try {
//            def songNames = Context.get().songDescriptorMapper.formatList(fetchedSongs)
//            FileTools.writeSongs(workDir, songNames, "${outputFilename}", true)
            FileTools.writeSongsJson(workDir, fetchedSongs, "${outputFilename}", true)
        } catch (Throwable ignore) {
            log.error("Fetched songs not saved: ")
            for (SongDescriptor sd : fetchedSongs) {
                log.error("           ${sd.artist} - ${sd.title}")
            }
        }
    }
}
