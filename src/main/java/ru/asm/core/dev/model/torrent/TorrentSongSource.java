package ru.asm.core.dev.model.torrent;

import ru.asm.core.dev.model.SongSource;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.util.MusicFormats;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 11:42
 */
public class TorrentSongSource implements SongSource {

    private String sourceId;

    private TorrentSongVO indexSong;

    public TorrentSongSource() {
    }

    public TorrentSongSource(TorrentSongVO indexSong) {
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

    @Override
    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public boolean isFlac() {
        return indexSong.getType().toUpperCase().equals(MusicFormats.FORMAT_FLAC);
    }

    public boolean isMp3() {
        return indexSong.getType().toUpperCase().equals(MusicFormats.FORMAT_MP3);
    }

    @Override
    public String getName() {
        if (isMp3()) {
            return String.format("%s: %s (%s)", indexSong.getArtistName(), indexSong.getSongName(), indexSong.getMp3FilePath());
        } else {
            return String.format("%s: %s (%s #%s)", indexSong.getArtistName(), indexSong.getSongName(), indexSong.getCueFilePath(), indexSong.getTrackNum());
        }
    }
}
