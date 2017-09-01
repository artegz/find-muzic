package ru.asm.core.dev.model;

import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.Mp3TorrentSongSource;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 11:43
 */
public interface SearchService {

    void resolveSongSources(Song song);

    List<Mp3TorrentSongSource> getSongSources(Song song);

    void downloadSongs(Song song, List<Mp3TorrentSongSource> sources);

    List<FileDocument> getDownloadedSongs(Song song);
}
