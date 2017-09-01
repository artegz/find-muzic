package ru.asm.core.dev.model;

import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.Mp3TorrentSongSource;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 31.08.2017
 * Time: 15:24
 */
public class SongInfo {

    private Song song;

    private List<Mp3TorrentSongSource> sources;

    private List<FileDocument> files;

    public List<FileDocument> getFiles() {
        return files;
    }

    public void setFiles(List<FileDocument> files) {
        this.files = files;
    }

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    public List<Mp3TorrentSongSource> getSources() {
        return sources;
    }

    public void setSources(List<Mp3TorrentSongSource> sources) {
        this.sources = sources;
    }
}
