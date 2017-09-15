package ru.asm.api.rest;

import com.google.common.collect.Lists;
import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.asm.core.AppConfiguration;
import ru.asm.core.AppCoreService;
import ru.asm.core.dev.model.*;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.persistence.domain.PlaylistSongEntity;
import ru.asm.core.persistence.domain.ResolvedSongEntity;
import ru.asm.core.persistence.domain.StatusEntity;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.progress.ProgressInfo;
import ru.asm.core.progress.ProgressListener;
import ru.asm.core.progress.ProgressService;
import ru.asm.core.progress.Task;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: artem.smirnov
 * Date: 22.08.2017
 * Time: 17:55
 */
@Path("/")
public class AppRestService {

    public static final Logger logger = LoggerFactory.getLogger(AppRestService.class);

    public static final String TASK_RESOLVE_SOURCES = "resolve sources";
    public static final String TASK_DOWNLOAD_SONGS = "download songs";

    @Autowired
    private SearchService searchService;

    @Autowired
    private PlaylistSongsMapper playlistSongsMapper;

    @Autowired
    private AppCoreService appCoreService;

    @Autowired
    private ProgressService progressService;

    private ExecutorService executionService = Executors.newFixedThreadPool(32);


    // Step 1.1: download fresh torrents DB
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs/download")
    public void downloadTorrentDb(@QueryParam("magnet") String magnet) {
        if (magnet == null) {
            magnet = "magnet:?xt=urn:btih:77D5646C8645A40BDB635FBED263B5B3A86BCB0F";
        }
        final AppConfiguration appConfiguration = AppConfiguration.getInstance();
        appCoreService.downloadTorrent(appConfiguration.getTorrentDbsStorageLocation(), magnet);
    }

    // Step 1.2: extract downloaded torrents DB
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs/extract")
    public void extractTorrentDb() {
        // todo: extract zip
    }

    // Step 1.3: index downloaded torrents DB
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs/index")
    public void indexTorrentDb() {
        final String pathname = "C:\\TEMP\\find-music\\rutracker_org_db\\backup.20170208185701\\backup.20170208185701.xml";
        final File backup = new File(pathname);

        appCoreService.indexTorrentsDb(backup);
    }

    // Step 2.1: download (fetch) desired playlist
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/playlists/download")
    public void downloadPlaylist() {
        // todo: download playlist
    }

    // Step 2.2: import playlist into DB
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/playlists/import")
    public void importPlaylist() {
        final String playlist = "nashe-test";
        final String comment = "test";
        final File file = new File("C:\\IdeaProjects\\find-muzic\\core\\src\\main\\resources\\playlists/test.nashe.playlist.txt");

        appCoreService.importPlaylist(playlist, comment, file);
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/artists")
    public List<Integer> getArtists() {
        return appCoreService.getArtistIds();
    }

    // Step 3: resolve artist torrents (find artists in torrents DB)
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/artists/search")
    public void searchArtists() {
        appCoreService.resolveArtists();
    }

    // Step 4: index songs from torrents
    // search for playlist artist songs in torrents (mp3 and flac files) and index them
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/songs/index")
    public void indexSongs(@QueryParam("format") String format) {
        if (format.equals("mp3")) {
            appCoreService.resolveSongs_mp3(null, null);
        } else if (format.equals("flac")) {
            appCoreService.resolveSongs_flac(null, null);
        }
    }

    // Step 5: resolve playlist songs (find desired sons in index)
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/songs/search")
    public void searchSongs() {
        appCoreService.resolveSongs();
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/songs")
    public List<ResolvedSongEntity> getFoundSongs() {
        return appCoreService.getFoundSongs();
    }

    // Step 6: download playlist songs
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/songs/download")
    public void downloadSongs() {
        // todo
        appCoreService.downloadFoundSongs_mp3(0, 10);
    }



    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/sample")
    public InputStream sample() {
        return getClass().getResourceAsStream("/sample.html");
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/artists")
    public List<String> artists() {
        final List<String> artists = playlistSongsMapper.getAllArtists();
        return artists;
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs")
    public List<String> getTorrentDbs() {
        final AppConfiguration appConfiguration = AppConfiguration.getInstance();

        final File[] files = appConfiguration.getTorrentDbsStorageLocation().listFiles(File::isFile);
        assert files != null;
        final List<String> filenames = Lists.transform(Arrays.asList(files), File::getName);

        return filenames;
    }


    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/results/statuses")
    public List<StatusEntity> getStatuses() {
        final List<StatusEntity> entities = playlistSongsMapper.getStatuses();
        return entities;
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/results/songs")
    public List<ResolvedSongEntity> getResolvedSongs() {
        return getFoundSongs();
    }




//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/artists")
//    public List<Artist> getArtistsAll() {
//        final Set<Artist> artists = new HashSet<>();
//        final List<PlaylistSongEntity> songs = this.playlistSongsMapper.getSongs("nashe-test");
//        for (PlaylistSongEntity song : songs) {
//            artists.add(getArtist(song));
//        }
//        return new ArrayList<>(artists);
//    }
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/artists/{artistId}/songs")
//    public List<Song> getArtistSongs(@PathParam("artistId") Integer artistId) {
//        final Set<Song> songs = new HashSet<>();
//        final List<PlaylistSongEntity> songEntities = this.playlistSongsMapper.getSongs("nashe-test");
//        for (PlaylistSongEntity songEntity : songEntities) {
//            if (songEntity.getArtistId().equals(artistId)) {
//                final Song song = getSong(songEntity);
//                songs.add(song);
//            }
//        }
//        return new ArrayList<>(songs);
//    }
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/songs")
//    public List<Song> getAllSongs() {
//        final Set<Song> songs = new HashSet<>();
//        final List<PlaylistSongEntity> songEntities = this.playlistSongsMapper.getSongs("nashe-test");
//        for (PlaylistSongEntity songEntity : songEntities) {
//            final Song song = getSong(songEntity);
//            songs.add(song);
//        }
//        return new ArrayList<>(songs);
//    }
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/playlists/{playlistId}/songs")
//    public List<Song> getPlaylistSongs(@QueryParam("playlistId") String playlistId) {
//        return getPlaylistSongs(playlistId);
//    }

//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/songs/{songId}/sources")
//    public List<Mp3TorrentSongSource> getSongSources(@PathParam("songId") Integer songId) {
//        PlaylistSongEntity foundSong = findSong(songId);
//
//        if (foundSong != null) {
//            final Song song = getSong(foundSong);
//            return this.searchService.getSongSources(song);
//        } else {
//            return null;
//        }
//    }




    // Step 1: see playlist
    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/playlists/{playlistId}/songs")
    public List<SongInfo> getPlaylistSongs2(@PathParam("playlistId") String playlistId) {
        final List<Song> songs = getPlaylistSongsImpl(playlistId);

        final List<SongInfo> songInfos = Lists.transform(songs, s -> {
            final SongInfo songInfo = new SongInfo();
            songInfo.setSong(s);
            songInfo.setSources(this.searchService.getSongSources(s));
            songInfo.setFiles(this.searchService.getDownloadedSongs(s));
            songInfo.setLastResolveReport(this.searchService.getLastSongResolveReport(s));
            return songInfo;
        });

        return songInfos;
    }

    // Step 2: resolve sources
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/songs/{songId}/sources/resolve")
    public void resolveSongSources(@PathParam("songId") Integer songId) {
        final Song song = findSongById(songId);
        if (song != null) {
            final ProgressListener progressListener = progressService.taskStarted(song, TASK_RESOLVE_SOURCES);
            this.searchService.resolveSongSources(song, progressListener);
            progressService.taskEnded(song);
        }
    }
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/songs/sources/resolve")
    public void resolveSongSources(List<Integer> songIds) {
        if (this.progressService.isInProgress()) {
            logger.error("unable start resolving, due to some tasks are already in progress");
            return;
        }

        final int total = songIds.size();
        final MutableInt complete = new MutableInt(1);
        logger.info("resolving {} songs", total);
        for (Integer songId : songIds) {
            executionService.submit(() -> {
                final Song song = findSongById(songId);
                if (song != null) {
                    final ProgressListener progressListener = progressService.taskStarted(song, TASK_RESOLVE_SOURCES);
                    logger.info("[{}/{}] resolving {} ({})...", complete, total, song.getFullName(), song.getSongId());
                    try {
                        this.searchService.resolveSongSources(song, progressListener);
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                    } finally {
                        complete.increment();
                        logger.info("[{}/{}] resolving {} ({}) complete", complete, total, song.getFullName(), song.getSongId());
                        progressService.taskEnded(song);
                    }
                } else {
                    complete.increment();
                    logger.error("song {} not found", songId);
                }
            });
        }
    }

    // Step 3: download
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/songs/{songId}/sources/download")
    public void downloadSongs(@PathParam("songId") Integer songId, DownloadInfo downloadInfo) {
        final Song song = findSongById(songId);

        if (song != null) {
            final ProgressListener progressListener = progressService.taskStarted(song, TASK_DOWNLOAD_SONGS);
            final List<TorrentSongSource> songSources = this.searchService.getSongSources(song);
            if (songSources != null) {
                final List<TorrentSongSource> specifiedSources = filter(songSources, downloadInfo.getSourcesIds());
                this.searchService.downloadSongs(song, specifiedSources, progressListener);
            }
            progressService.taskEnded(song);
        }
    }
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/songs/sources/download")
    public void downloadSongs(Map<Integer, List<String>> songsDownloadInfos) {
        logger.info("downloading {} songs", songsDownloadInfos.size());
        final MutableInt complete = new MutableInt(1);
        for (Integer songId : songsDownloadInfos.keySet()) {
            executionService.submit(() -> {
                final Song song = findSongById(songId);
                final List<String> downloadInfo = songsDownloadInfos.get(songId);

                if (song != null) {
                    logger.info("downloading {} ({}) from {} sources...", song.getFullName(), song.getSongId(), downloadInfo.size());

                    final ProgressListener progressListener = progressService.taskStarted(song, TASK_DOWNLOAD_SONGS);
                    final List<TorrentSongSource> songSources = this.searchService.getSongSources(song);
                    if (songSources != null) {
                        final List<TorrentSongSource> specifiedSources = filter(songSources, downloadInfo);
                        this.searchService.downloadSongs(song, specifiedSources, progressListener);
                    }
                    progressService.taskEnded(song);

                    complete.increment();
                    logger.info("resolving complete ({} / {})", complete, songsDownloadInfos.size());
                } else {
                    complete.increment();
                    logger.error("song {} not found", songId);
                }
            });
        }
    }



    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/progresses")
    public ProgressInfo getProgressInfo() {
        final Map<PlaylistSongEntity, Task> progresses = new HashMap<>();
        final Map<Integer, Task> tasksInProgress = progressService.getTasksInProgress();
        for (Integer songId : tasksInProgress.keySet()) {
            final PlaylistSongEntity songEntity = this.playlistSongsMapper.getSong(songId);
            progresses.put(songEntity, tasksInProgress.get(songId));
        }
        final List<PlaylistSongEntity> sortedSongs = new ArrayList<>(progresses.keySet());
        sortedSongs.sort(Comparator.comparing(PlaylistSongEntity::getSongId));
        return new ProgressInfo(sortedSongs, tasksInProgress);
    }



    private List<TorrentSongSource> filter(List<TorrentSongSource> songSources, List<String> sourcesIds) {
        final List<TorrentSongSource> specifiedSources = new ArrayList<>();
        for (TorrentSongSource songSource : songSources) {
            if (sourcesIds.contains(songSource.getSourceId())) {
                specifiedSources.add(songSource);
            }
        }
        return specifiedSources;
    }


    private PlaylistSongEntity findSong(Integer songId) {
        return this.playlistSongsMapper.getSong(songId);
    }

    private Song getSong(PlaylistSongEntity foundSong) {
        final Song song = new Song();
        final Artist artist = getArtist(foundSong);
        song.setSongId(foundSong.getSongId());
        song.setTitle(foundSong.getTitle());
        song.setArtist(artist);
        return song;
    }

    private Artist getArtist(PlaylistSongEntity foundSong) {
        final Artist artist = new Artist();
        artist.setArtistId(foundSong.getArtistId());
        artist.setArtistName(foundSong.getArtist());
        return artist;
    }


    private List<Song> getPlaylistSongsImpl(String playlistId) {
        final Set<Song> songs = new HashSet<>();
        final List<PlaylistSongEntity> songEntities = this.playlistSongsMapper.getSongs(playlistId);
        for (PlaylistSongEntity songEntity : songEntities) {
            final Song song = getSong(songEntity);
            songs.add(song);
        }
        return new ArrayList<>(songs);
    }

    private Song findSongById(Integer songId) {
        final Song song;
        PlaylistSongEntity foundSong = findSong(songId);
        if (foundSong != null) {
            song = getSong(foundSong);
        } else {
            song = null;
        }
        return song;
    }
}
