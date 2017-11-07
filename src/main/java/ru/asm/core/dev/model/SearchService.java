package ru.asm.core.dev.model;

import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.progress.TaskProgress;

import java.util.List;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 11:43
 */
public interface SearchService {

    void indexArtist(Artist artist, TaskProgress taskProgress);

    void searchSong(Artist artist, Song song, TaskProgress taskProgress);

    List<TorrentSongSource> getSongSources(Song song);

    ArtistResolveReport getArtistResolveReport(Artist artist);

    void downloadTorrent(String torrentId, Map<Song, List<TorrentSongSource>> downloadRequest, TaskProgress taskProgress);

    List<FileDocument> getSongDownloadedFiles(Song song);
}
