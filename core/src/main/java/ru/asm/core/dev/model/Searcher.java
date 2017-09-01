package ru.asm.core.dev.model;

import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 12:06
 */
public interface Searcher {

    void resolveSongSources(Song song);

    List<TorrentSongSource> getSongSources(Song song);

    void downloadSongs(Song song, List<TorrentSongSource> sources);

    List<FileDocument> getDownloadedSongs(Song song);
}
