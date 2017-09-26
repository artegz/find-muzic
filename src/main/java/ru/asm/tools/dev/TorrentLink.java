package ru.asm.tools.dev;

import ru.asm.core.index.domain.TorrentFilesVO;
import ru.asm.core.index.domain.TorrentSongVO;

/**
 * User: artem.smirnov
 * Date: 25.08.2017
 * Time: 18:13
 */
public class TorrentLink {

    private TorrentFilesVO torrent;

    private TorrentSongVO torrentSong;

    public TorrentLink(TorrentFilesVO torrent, TorrentSongVO torrentSong) {
        this.torrent = torrent;
        this.torrentSong = torrentSong;
    }

    public TorrentFilesVO getTorrent() {
        return torrent;
    }

    public TorrentSongVO getTorrentSong() {
        return torrentSong;
    }
}
