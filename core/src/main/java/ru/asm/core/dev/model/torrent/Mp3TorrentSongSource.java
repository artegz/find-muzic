package ru.asm.core.dev.model.torrent;

import ru.asm.core.dev.model.SongSource;
import ru.asm.core.index.domain.TorrentSongVO;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 11:42
 */
public class Mp3TorrentSongSource implements SongSource {

    private TorrentSongVO indexSong;

    public Mp3TorrentSongSource(TorrentSongVO indexSong) {

        this.indexSong = indexSong;
    }
}
