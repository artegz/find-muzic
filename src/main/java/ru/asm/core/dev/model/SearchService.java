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

    void resolveSongSources(Song song, TaskProgress taskProgress);
    void resolveSongSources(Artist artist, List<Song> songs, TaskProgress taskProgress);

    void indexArtist(Artist artist, TaskProgress taskProgress);

    void searchSong(Artist artist, Song song, TaskProgress taskProgress);

    List<TorrentSongSource> getSongSources(Song song);

    ArtistResolveReport getArtistResolveReport(Artist artist);

    void downloadSongs(Song song, List<TorrentSongSource> sources, TaskProgress taskProgress);
    void downloadSongs(Artist artist, Map<Song, List<TorrentSongSource>> downloadRequest, TaskProgress taskProgress);
    void downloadTorrent(String torrentId, Map<Song, List<TorrentSongSource>> downloadRequest, TaskProgress taskProgress);

    List<FileDocument> getSongDownloadedFiles(Song song);
}
