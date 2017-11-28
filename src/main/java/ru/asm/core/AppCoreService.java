package ru.asm.core;

import org.springframework.transaction.annotation.Transactional;
import ru.asm.core.dev.model.Artist;
import ru.asm.core.dev.model.ArtistResolveReport;
import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.ddb.SongSourceDocument;
import ru.asm.core.dev.model.ddb.TorrentDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.progress.TaskProgress;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: artem.smirnov
 * Date: 28.11.2017
 * Time: 8:32
 */
public interface AppCoreService {

    void indexArtist(Artist artist, TaskProgress taskProgress);

    void searchSong(Artist artist, Song song, TaskProgress taskProgress);

    void downloadTorrent(String torrentId, Map<Song, List<TorrentSongSource>> downloadRequest, TaskProgress taskProgress);

    List<FileDocument> getDownloadedSongs(Song song);

    List<TorrentSongSource> getSongSources(Song song);

    ArtistResolveReport getArtistResolveReport(Artist artist);

    SongSourceDocument getSourceById(String sourceId);

    List<FileDocument> getFilesBySongId(Integer songId);

    List<SongSourceDocument> getSongSourcesByTorrentId(String torrentId);

    TorrentDocument getTorrentById(String torrentId);

    Song getSongById(Integer songId);

    Artist getArtistById(Integer artistId);

    Set<Artist> getPlaylistArtists(String playlistId);

    List<Song> getPlaylistSongs(@SuppressWarnings("SameParameterValue") String playlistId);

    void downloadTorrent(File folder, String magnet);

    @Transactional
    void importPlaylist(String playlistName, String comment, File playlist);

    void indexTorrentsDb(File backup);
}
