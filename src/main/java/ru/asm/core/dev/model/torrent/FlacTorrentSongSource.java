package ru.asm.core.dev.model.torrent;

import ru.asm.core.dev.model.SongSource;
import ru.asm.core.index.domain.TorrentSongVO;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 11:42
 */
public class FlacTorrentSongSource implements SongSource {

    private String sourceId;

    private TorrentSongVO indexSong;

    public FlacTorrentSongSource() {
    }

    public FlacTorrentSongSource(TorrentSongVO indexSong) {
        this.sourceId = indexSong.getId();
        this.indexSong = indexSong;
    }

    public TorrentSongVO getIndexSong() {
        return indexSong;
    }

    public void setIndexSong(TorrentSongVO indexSong) {
        this.indexSong = indexSong;
        this.sourceId = indexSong.getId();
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
}
