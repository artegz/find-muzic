package ru.asm.tools;

import com.frostwire.jlibtorrent.TorrentInfo;
import ru.asm.core.torrent.TorrentClient;

import java.io.File;
import java.io.IOException;

/**
 * User: artem.smirnov
 * Date: 14.03.2017
 * Time: 14:05
 */
class TorrentsDbDownloadTool {

    public static void main(String[] args) {
//        String magnet = "magnet:?xt=urn:btih:77D5646C8645A40BDB635FBED263B5B3A86BCB0F&tr=http%3A%2F%2Fbt2.t-ru.org%2Fann%3Fmagnet&dn=XML%20%D0%B1%D0%B0%D0%B7%D0%B0%20%D1%80%D0%B0%D0%B7%D0%B4%D0%B0%D1%87%20RuTracker.ORG%20v.0.1.20170208";
        String magnet = "magnet:?xt=urn:btih:77D5646C8645A40BDB635FBED263B5B3A86BCB0F";
        File folder = new File("C:\\TEMP\\find-music\\rutracker_org_db");

        if (!folder.exists()) folder.mkdirs();

        TorrentClient torrentClient = new TorrentClient();


        try {
            torrentClient.initializeSession();

            TorrentInfo torrentInfo = torrentClient.findByMagnet(magnet);

            String name = torrentInfo.name();
            File resumeFile = new File(folder, name + ".tmp");
            if (!resumeFile.exists()) {
                try {
                    resumeFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            torrentClient.download(name, torrentInfo, folder, null, resumeFile);
        } finally {
            torrentClient.destroySession();
        }
    }
}
