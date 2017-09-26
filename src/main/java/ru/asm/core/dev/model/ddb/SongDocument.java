package ru.asm.core.dev.model.ddb;

import org.dizitart.no2.objects.Id;
import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.torrent.TorrentSongSource;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 31.08.2017
 * Time: 14:49
 */
public class SongDocument {

    @Id
    private Long id;

    private Integer songId;

    private Song song;

    private List<TorrentSongSource> songSources;

    public Long getId() {
        return id;
    }

    public Integer getSongId() {
        return songId;
    }

    public Song getSong() {
        return song;
    }

    public List<TorrentSongSource> getSongSources() {
        return songSources;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSongId(Integer songId) {
        this.songId = songId;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    public void setSongSources(List<TorrentSongSource> songSources) {
        this.songSources = songSources;
    }
}
