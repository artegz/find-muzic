package ru.asm.core.dev.model;

import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;

/**
 * User: artem.smirnov
 * Date: 05.10.2017
 * Time: 9:39
 */
public class FileInfoVO {

    private Song song;

    private TorrentSongSource songSource;

    private FileDocument file;

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

    public FileDocument getFile() {
        return file;
    }

    public void setFile(FileDocument file) {
        this.file = file;
    }
}
