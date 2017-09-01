package ru.asm.core.dev.model.ddb;

import ru.asm.core.dev.model.torrent.Mp3TorrentSongSource;

/**
 * User: artem.smirnov
 * Date: 31.08.2017
 * Time: 16:07
 */
public class SongSourceDocument {

    private Long id;

    private Integer songId;

    private String sourceId;

    private Mp3TorrentSongSource songSource;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSongId() {
        return songId;
    }

    public void setSongId(Integer songId) {
        this.songId = songId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Mp3TorrentSongSource getSongSource() {
        return songSource;
    }

    public void setSongSource(Mp3TorrentSongSource songSource) {
        this.songSource = songSource;
    }
}
