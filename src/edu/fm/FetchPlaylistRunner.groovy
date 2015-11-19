package edu.fm

import java.text.SimpleDateFormat

/**
 * User: artem.smirnov
 * Date: 19.11.2015
 * Time: 10:52
 */
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

        FileTools.writeSongs(workDir, fetchedSongs, "${outputFilename}", true)
    }
}
