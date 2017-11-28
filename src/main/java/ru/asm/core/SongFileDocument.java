package ru.asm.core;

import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;

/**
 * User: artem.smirnov
 * Date: 07.11.2017
 * Time: 10:53
 */
class SongFileDocument {

    private Song song;

    private TorrentSongSource songSource;

    private FileDocument fileDocument;

    SongFileDocument(Song song, TorrentSongSource songSource, FileDocument fileDocument) {
        this.song = song;
        this.songSource = songSource;
        this.fileDocument = fileDocument;
    }

    Song getSong() {
        return song;
    }

    FileDocument getFileDocument() {
        return fileDocument;
    }

    TorrentSongSource getSongSource() {
        return songSource;
    }
}
