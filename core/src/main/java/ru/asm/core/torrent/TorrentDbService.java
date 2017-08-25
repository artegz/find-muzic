package ru.asm.core.torrent;

import com.frostwire.jlibtorrent.TorrentInfo;

import java.io.File;
import java.io.IOException;

/**
 * User: artem.smirnov
 * Date: 25.08.2017
 * Time: 15:40
 */
public class TorrentDbService {

    public void download(File folder, String magnet) {
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
                    System.out.println(e.getMessage());
                }
            }

            torrentClient.download(torrentInfo, folder, null);
        } finally {
            torrentClient.destroySession();
        }
    }
}
