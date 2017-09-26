package ru.asm.core.dev.model;

import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 31.08.2017
 * Time: 15:24
 */
public class SongInfo {

    private Song song;

    private List<TorrentSongSource> sources;

    private List<FileDocument> files;

    private SongResolveReport lastResolveReport;

    public SongResolveReport getLastResolveReport() {
        return lastResolveReport;
    }

    public void setLastResolveReport(SongResolveReport lastResolveReport) {
        this.lastResolveReport = lastResolveReport;
    }

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

    public List<TorrentSongSource> getSources() {
        return sources;
    }

    public void setSources(List<TorrentSongSource> sources) {
        this.sources = sources;
    }
}
