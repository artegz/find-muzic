package ru.asm.tools

import ru.asm.torrent.TorrentClient
/**
 * User: artem.smirnov
 * Date: 14.03.2017
 * Time: 14:05
 */
class TorrentsDbDownloadTool {

    static void main(String[] args) {
        //def magnet = "magnet:?xt=urn:btih:77D5646C8645A40BDB635FBED263B5B3A86BCB0F&tr=http%3A%2F%2Fbt2.t-ru.org%2Fann%3Fmagnet&dn=XML%20%D0%B1%D0%B0%D0%B7%D0%B0%20%D1%80%D0%B0%D0%B7%D0%B4%D0%B0%D1%87%20RuTracker.ORG%20v.0.1.20170208"
        def magnet = "magnet:?xt=urn:btih:77D5646C8645A40BDB635FBED263B5B3A86BCB0F"
        def folder = new File("C:\\TEMP\\find-music\\rutracker_org_db")

        if (!folder.exists()) folder.mkdirs()

        def torrentClient = new TorrentClient()

        def torrentInfo = torrentClient.findByMagnet(magnet)

        def name = torrentInfo.name()
        def resumeFile = new File(folder, name + ".tmp")
        if (!resumeFile.exists()) {
            resumeFile.createNewFile()
        }

        torrentClient.download(name, torrentInfo, folder, null, resumeFile)
    }
}
