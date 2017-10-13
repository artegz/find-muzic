package ru.asm.core.dev.model;

import ru.asm.core.dev.model.torrent.TorrentSongSource;

/**
 * User: artem.smirnov
 * Date: 05.10.2017
 * Time: 9:38
 */
public class LiteSourceInfo {

    private Song song;
    private TorrentSongSource songSource;
    private OperationStatus downloadStatus;
    private int numFiles;

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    public TorrentSongSource getSongSource() {
        return songSource;
    }

    public void setSongSource(TorrentSongSource songSource) {
        this.songSource = songSource;
    }

    public OperationStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(OperationStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }
}
