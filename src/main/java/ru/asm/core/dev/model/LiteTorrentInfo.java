package ru.asm.core.dev.model;

import ru.asm.core.dev.model.torrent.TorrentSongSource;

import java.util.ArrayList;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 11.10.2017
 * Time: 9:47
 */
public class LiteTorrentInfo {

    private String torrentId;
    private String title;
    private String format;

    private List<Song> containedSongs = new ArrayList<>();

    private List<TorrentSongSource> containedSources = new ArrayList<>();

    public String getTorrentId() {
        return torrentId;
    }

    public void setTorrentId(String torrentId) {
        this.torrentId = torrentId;
    }

    public List<Song> getContainedSongs() {
        return containedSongs;
    }

    public void setContainedSongs(List<Song> containedSongs) {
        this.containedSongs = containedSongs;
    }

    public List<TorrentSongSource> getContainedSources() {
        return containedSources;
    }

    public void setContainedSources(List<TorrentSongSource> containedSources) {
        this.containedSources = containedSources;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
