package ru.asm.api.rest;

import com.google.common.collect.Lists;
import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.asm.core.AppConfiguration;
import ru.asm.core.AppCoreService;
import ru.asm.core.dev.model.*;
import ru.asm.core.dev.model.ddb.DataStorage;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.ddb.SongSourceDocument;
import ru.asm.core.dev.model.ddb.TorrentDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.persistence.domain.PlaylistSongEntity;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.progress.ProgressService;
import ru.asm.core.progress.SimpleProgressInfo;
import ru.asm.core.progress.TaskProgress;

import javax.ws.rs.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * User: artem.smirnov
 * Date: 22.08.2017
 * Time: 17:55
 */
@Path("/")
public class AppRestService {

    public static final Logger logger = LoggerFactory.getLogger(AppRestService.class);

    public static final String TASK_INDEX_ARTISTS = "index artists";
//    public static final String TASK_RESOLVE_SOURCES = "resolve sources";
    public static final String SEARCH_SONGS = "search songs";
//    public static final String TASK_DOWNLOAD_SONGS = "download songs";
    public static final String TASK_DOWNLOAD_TORRENTS = "download torrents";

    @Autowired
    private SearchService searchService;

    @Autowired
    private DataStorage dataStorage;

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
        appCoreService.downloadTorrent(appConfiguration.getTorrentBackupDownloadFile(), magnet);
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
        final String pathname = AppConfiguration.TORRENTS_BACKUP_XML_LOCATION;
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
        final String playlist = "nashe";
        final String comment = "test";
        final File file = new File(AppConfiguration.DEFAULT_PLAYLIST_FILE_LOCATION);

        appCoreService.importPlaylist(playlist, comment, file);
    }
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/artists")
//    public List<Integer> getArtists() {
//        return appCoreService.getArtistIds();
//    }
//
//    // Step 3: resolve artist torrents (find artists in torrents DB)
//    @POST
//    @Produces("application/json; charset=UTF-8")
//    @Path("/artists/search")
//    public void searchArtists() {
//        appCoreService.resolveArtists();
//    }
//
//    // Step 4: index songs from torrents
//    // search for playlist artist songs in torrents (mp3 and flac files) and index them
//    @POST
//    @Produces("application/json; charset=UTF-8")
//    @Path("/songs/index")
//    public void indexSongs(@QueryParam("format") String format) {
//        if (format.equals("mp3")) {
//            appCoreService.resolveSongs_mp3(null, null);
//        } else if (format.equals("flac")) {
//            appCoreService.resolveSongs_flac(null, null);
//        }
//    }
//
//    // Step 5: resolve playlist songs (find desired sons in index)
//    @POST
//    @Produces("application/json; charset=UTF-8")
//    @Path("/songs/search")
//    public void searchSongs() {
//        appCoreService.resolveSongs();
//    }
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/songs")
//    public List<ResolvedSongEntity> getFoundSongs() {
//        return appCoreService.getFoundSongs();
//    }
//
//    // Step 6: download playlist songs
//    @POST
//    @Produces("application/json; charset=UTF-8")
//    @Path("/songs/download")
//    public void downloadSongs() {
//        // todo
//        appCoreService.downloadFoundSongs_mp3(0, 10);
//    }

//
//
//    @GET
//    @Produces(MediaType.TEXT_HTML)
//    @Path("/sample")
//    public InputStream sample() {
//        return getClass().getResourceAsStream("/sample.html");
//    }
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/artists")
//    public List<String> artists() {
//        final List<String> artists = playlistSongsMapper.getAllArtists();
//        return artists;
//    }

//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/torrentDbs")
//    public List<String> getTorrentDbs() {
//        final AppConfiguration appConfiguration = AppConfiguration.getInstance();
//
//        final File[] files = appConfiguration.getTorrentBackupDownloadFile().listFiles(File::isFile);
//        assert files != null;
//        final List<String> filenames = Lists.transform(Arrays.asList(files), File::getName);
//
//        return filenames;
//    }
//
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/results/statuses")
//    public List<StatusEntity> getStatuses() {
//        final List<StatusEntity> entities = playlistSongsMapper.getStatuses();
//        return entities;
//    }
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/results/songs")
//    public List<ResolvedSongEntity> getResolvedSongs() {
//        return getFoundSongs();
//    }
//



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
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/playlists/{playlistId}/songs")
//    public List<SongInfo> getPlaylistSongs2(@PathParam("playlistId") String playlistId,
//                                            @QueryParam("includeSources") Boolean includeSources,
//                                            @QueryParam("includeFiles") Boolean includeFiles,
//                                            @QueryParam("includeResolveReport") Boolean includeResolveReport,
//                                            @QueryParam("includeDownloadReport") Boolean includeDownloadReport) {
//        final List<Song> songs = getPlaylistSongsImpl(playlistId);
//
//        final List<SongInfo> songInfos = Lists.transform(songs, s -> {
//            final SongInfo songInfo = new SongInfo();
//            songInfo.setSong(s);
//            if (includeSources) {
//                songInfo.setSources(this.searchService.getSongSources(s));
//            }
//            if (includeFiles) {
//                songInfo.setFiles(this.searchService.getSongDownloadedFiles(s));
//            }
//            if (includeResolveReport) {
//                assert (s != null);
//                songInfo.setLastResolveReport(this.searchService.getArtistResolveReport(s.getArtist()));
//            }
//            return songInfo;
//        });
//
//        return songInfos;
//    }








    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/index/artists")
    public List<LiteArtistInfo> getArtistsForIndex() {
        final List<Song> songs = getPlaylistSongsImpl("nashe");
        final Set<Artist> artists = songs.stream()
                .map(Song::getArtist)
                .collect(Collectors.toSet());

        final List<LiteArtistInfo> liteArtistInfos = new ArrayList<>();
        for (Artist artist : artists) {
            final LiteArtistInfo liteArtistInfo = new LiteArtistInfo();
            liteArtistInfo.setArtist(artist);

            final ArtistResolveReport artistResolveReport = this.searchService.getArtistResolveReport(artist);
            if (artistResolveReport != null) {
                if (artistResolveReport.isIndexingSucceeded()) {
                    liteArtistInfo.setIndexingStatus(OperationStatus.succeeded);
                } else {
                    if (artistResolveReport.getResolvePerformed() || artistResolveReport.getIndexingPerformed()) {
                        liteArtistInfo.setIndexingStatus(OperationStatus.failed);
                    } else {
                        liteArtistInfo.setIndexingStatus(OperationStatus.unkwnown);
                    }
                }
            } else {
                liteArtistInfo.setIndexingStatus(OperationStatus.unkwnown);
            }

            liteArtistInfos.add(liteArtistInfo);
        }

        liteArtistInfos.sort(Comparator.comparing(o -> o.getArtist().getArtistName()));

        return liteArtistInfos;
    }
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/index/artists/start")
    public void indexArtists(List<Integer> artistIds) {
        if (this.progressService.isInProgress()) {
            logger.error("unable start resolving, due to some tasks are already in progress");
            return;
        }

        final int total = artistIds.size();
        final MutableInt complete = new MutableInt(0);

        logger.info("indexing {} artists", total, artistIds.size());

        // queue all tasks to be done
        progressService.queueTasks(artistIds
                .stream()
                .map(ProgressService::indexArtistTaskId)
                .collect(Collectors.toList()), TASK_INDEX_ARTISTS);

        for (Integer artistId : artistIds) {
            executionService.submit(() -> {
                final String taskId = ProgressService.indexArtistTaskId(artistId);

                try {
                    logger.info("indexing artist {}", artistId);
                    final Artist artist = getArtistById(artistId);

                    logger.info("[{}/{}] indexing...", complete, total);

                    // start concrete task
                    final TaskProgress taskProgress = progressService.taskStarted(
                            taskId,
                            String.format("indexing '%s' artist", artist.getArtistName()),
                            artist,
                            null,
                            TASK_INDEX_ARTISTS
                    );

                    this.searchService.indexArtist(artist, taskProgress);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    // end task
                    complete.add(1);
                    logger.info("[{}/{}] resolving complete", complete, total);
                    progressService.taskEnded(taskId, TASK_INDEX_ARTISTS);
                }
            });
        }
    }



    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/search/songs")
    public List<LiteSongInfo> getSongsForSearch() {
        final List<Song> songs = getPlaylistSongsImpl("nashe");

        //noinspection UnnecessaryLocalVariable
        final List<LiteSongInfo> songInfos = new ArrayList<>(Lists.transform(songs, s -> {
            assert (s != null);

            final ArtistResolveReport artistResolveReport = this.searchService.getArtistResolveReport(s.getArtist());
            final List<TorrentSongSource> songSources = this.searchService.getSongSources(s);
            final List<FileDocument> songDownloadedFiles = this.searchService.getSongDownloadedFiles(s);

            final LiteSongInfo liteSongInfo = new LiteSongInfo();

            liteSongInfo.setSong(s);
            liteSongInfo.setNumFiles(songDownloadedFiles.size());
            liteSongInfo.setNumSources(songSources.size());

            // determine status
            final OperationStatus resolveStatus;
            if (artistResolveReport != null) {
                if (artistResolveReport.getSearchPerformed()) {
                    if (songSources.isEmpty()) {
                        resolveStatus = OperationStatus.failed;
                    } else {
                        resolveStatus = OperationStatus.succeeded;
                    }
                } else {
                    if (artistResolveReport.getIndexingPerformed() || artistResolveReport.getResolvePerformed()) {
                        resolveStatus = OperationStatus.failed;
                    } else {
                        resolveStatus = OperationStatus.unkwnown;
                    }
                }
            } else {
                resolveStatus = OperationStatus.unkwnown;
            }

            liteSongInfo.setResolveStatus(resolveStatus);

            return liteSongInfo;
        }));

        songInfos.sort(Comparator.comparing(o -> o.getSong().getFullName()));

        return songInfos;
    }
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/search/songs/start")
    public void searchSongs(List<Integer> songIds) {
        if (this.progressService.isInProgress()) {
            logger.error("unable start resolving, due to some tasks are already in progress");
            return;
        }

        final int total = songIds.size();
        final MutableInt complete = new MutableInt(0);

        for (Integer songId : songIds) {
            executionService.submit(() -> {
                final Song song = findSongById(songId);
                final String taskId = ProgressService.searchSongTaskId(songId);

                try {
                    logger.info("[{}/{}] searching...", complete, total);

                    // start concrete task
                    final TaskProgress taskProgress = progressService.taskStarted(
                            taskId,
                            String.format("searching song '%s'", song.getFullName()),
                            song.getArtist(),
                            Collections.singletonList(song),
                            SEARCH_SONGS
                    );

                    this.searchService.searchSong(song.getArtist(), song, taskProgress);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    // end task
                    complete.add(1);
                    logger.info("[{}/{}] searching complete", complete, total);
                    progressService.taskEnded(taskId, SEARCH_SONGS);
                }
            });
        }
    }



    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/download/torrents")
    public List<LiteTorrentInfo> getTorrentsForDownload() {
        final List<Song> songs = getPlaylistSongsImpl("nashe");

        final Map<String, LiteTorrentInfo> torrentInfos = new HashMap<>();

        for (Song song : songs) {
            final List<TorrentSongSource> sources = searchService.getSongSources(song);

            for (TorrentSongSource source : sources) {
                final String torrentId = source.getIndexSong().getTorrentId();

                if (!torrentInfos.containsKey(torrentId)) {
                    final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

                    final LiteTorrentInfo torrentInfo = new LiteTorrentInfo();
                    torrentInfo.setTorrentId(torrentId);
                    torrentInfo.setTitle(torrent.getTorrentInfo().getTitle());
                    torrentInfo.setFormat(torrent.getFormat());

                    torrentInfos.put(torrentId, torrentInfo);
                }

                torrentInfos.get(torrentId).getContainedSongs().add(song);
                torrentInfos.get(torrentId).getContainedSources().add(source);
            }
        }

        final List<LiteTorrentInfo> result = new ArrayList<>(torrentInfos.values());
        result.sort(Comparator.comparing(LiteTorrentInfo::getTitle));

        return result;
    }
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/download/torrents/start")
    public void downloadTorrents(Map<String, List<String>> torrentSources) {
        final Set<String> torrentIds = torrentSources.keySet();

        final int total = torrentIds.size();
        final MutableInt complete = new MutableInt(0);

        logger.info("downloading {} torrents", total);

        // queue all tasks to be done
        progressService.queueTasks(
                torrentIds.stream()
                        .map(ProgressService::downloadTorrentsTaskId)
                        .collect(Collectors.toList()),
                TASK_DOWNLOAD_TORRENTS
        );

        for (String torrentId : torrentIds) {
            executionService.submit(() -> {
                final String taskId = ProgressService.downloadTorrentsTaskId(torrentId);

                try {
                    logger.info("downloading torrent {}...", torrentId);

                    final TaskProgress taskProgress = progressService.taskStarted(
                            taskId,
                            String.format("downloading torrent '%s'", torrentId),
                            null,
                            null,
                            TASK_DOWNLOAD_TORRENTS
                    );

                    final Map<Song, List<TorrentSongSource>> downloadRequest = prepareDownloadRequestSingleTorrent(torrentId, torrentSources.get(torrentId));
                    this.searchService.downloadTorrent(torrentId, downloadRequest, taskProgress);
                } finally {
                    complete.add(1);
                    logger.info("downloading complete ({} / {})", complete, total);

                    progressService.taskEnded(taskId, TASK_DOWNLOAD_TORRENTS);
                }
            });
        }
    }



    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/build/files")
    public List<LiteFileInfo> getDownloadedFiles() {
        final List<Song> songs = getPlaylistSongsImpl("nashe");

        final List<LiteFileInfo> fileInfos = new ArrayList<>();

        for (Song song : songs) {
            final List<FileDocument> files = dataStorage.getFiles(song.getSongId());

            for (FileDocument file : files) {
                final SongSourceDocument songSource = dataStorage.getSongSource(file.getSourceId());

                final LiteFileInfo liteFileInfo = new LiteFileInfo();
                liteFileInfo.setSong(song);
                liteFileInfo.setFile(file);
                liteFileInfo.setSongSource(songSource.getSongSource());

                fileInfos.add(liteFileInfo);
            }
        }

        fileInfos.sort(Comparator.comparing(o -> o.getSong().getFullName()));

        return fileInfos;
    }
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/build/files/start")
    public void buildPlaylist(List<Long> fileIds) {
        final List<LiteFileInfo> downloadedFiles = getDownloadedFiles();
        final List<LiteFileInfo> requestedFiles = downloadedFiles.stream()
                .filter(liteFileInfo -> fileIds.contains(liteFileInfo.getFile().getId()))
                .collect(Collectors.toList());


        final File outputFolder = new File(AppConfiguration.OUTPUT_FOLDER);


        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        for (LiteFileInfo requestedFile : requestedFiles) {
            final File songFile = new File(requestedFile.getFile().getFsLocation());
            final File destFile = new File(outputFolder, songFile.getName());
            try {
                Files.copy(songFile.toPath(), destFile.toPath(), REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    // Step 2: resolve sources
//    @POST
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/songs/{songId}/sources/resolve")
//    public void searchSongs(@PathParam("songId") Integer songId) {
//        final Song song = findSongById(songId);
//        if (song != null) {
//            final ProgressListener progressListener = progressService.taskStarted(song, TASK_RESOLVE_SOURCES);
//            this.searchService.searchSongs(song, progressListener);
//            progressService.taskEnded(song);
//        }
//    }


    // Step 3: download
//    @POST
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/songs/{songId}/sources/download")
//    public void downloadSongs(@PathParam("songId") Integer songId, DownloadInfo downloadInfo) {
//        final Song song = findSongById(songId);
//
//        if (song != null) {
//            final ProgressListener progressListener = progressService.taskStarted(song, TASK_DOWNLOAD_SONGS);
//            final List<TorrentSongSource> songSources = this.searchService.getSongSources(song);
//            if (songSources != null) {
//                final List<TorrentSongSource> specifiedSources = filter(songSources, downloadInfo.getSourcesIds());
//                this.searchService.downloadSongs(song, specifiedSources, progressListener);
//            }
//            progressService.taskEnded(song);
//        }
//    }
//    @POST
//    @Produces("application/json; charset=UTF-8")
//    @Path("/alt/songs/sources/download")
//    public void downloadSongs(Map<Integer, List<String>> songsDownloadInfos) {
//        final int total = songsDownloadInfos.size();
//        final MutableInt complete = new MutableInt(0);
//
//        logger.info("downloading {} songs", total);
//
//        // group songs per artist
//        final Map<Integer, List<Song>> songsPerArtist = groupPerArtist(songsDownloadInfos.keySet());
//
//        // all artist ids
//        final Set<Integer> artistIds = songsPerArtist.keySet();
//
//        // queue all tasks to be done
//        progressService.queueTasks(
//                artistIds.stream()
//                        .map(ProgressService::downloadArtistSongsTaskId)
//                        .collect(Collectors.toList()),
//                TASK_DOWNLOAD_SONGS
//        );
//
//        for (Integer artistId : artistIds) {
//            executionService.submit(() -> {
//                final List<Song> artistSongs = songsPerArtist.get(artistId);
//                final String taskId = ProgressService.downloadArtistSongsTaskId(artistId);
//
//                try {
//                    final Artist artist = getSongsArtist(artistSongs);
//                    logger.info("downloading {} songs of {}...", artistSongs.size(), artist.getArtistName());
//
//                    final TaskProgress taskProgress = progressService.taskStarted(
//                            taskId,
//                            String.format("downloading '%s' songs", artist.getArtistName()),
//                            artist,
//                            artistSongs,
//                            TASK_DOWNLOAD_SONGS
//                    );
//
//                    final Map<Song, List<TorrentSongSource>> downloadRequest = prepareDownloadRequest(songsDownloadInfos, artistSongs);
//                    this.searchService.downloadSongs(artist, downloadRequest, taskProgress);
//                } finally {
//                    complete.add(artistSongs.size());
//                    logger.info("resolving complete ({} / {})", complete, total);
//
//                    progressService.taskEnded(taskId, TASK_DOWNLOAD_SONGS);
//                }
//            });
//        }
//    }
//
//
//
//    @GET
//    @Produces("application/json; charset=UTF-8")
//    @Path("/progresses")
//    public ProgressInfo getProgressInfo() {
//        final Map<PlaylistSongEntity, Task> progresses = new HashMap<>();
//        final Map<Integer, Task> tasksInProgress = new HashMap<>();//progressService.getTasksInProgress();
//        Set<Integer> songIds = new HashSet<>(tasksInProgress.keySet());
//        for (Integer songId : songIds) {
//            final PlaylistSongEntity songEntity = this.playlistSongsMapper.getSong(songId);
//            progresses.put(songEntity, tasksInProgress.get(songId));
//        }
//        final List<PlaylistSongEntity> sortedSongs = new ArrayList<>(progresses.keySet());
//        sortedSongs.sort(Comparator.comparing(PlaylistSongEntity::getSongId));
//        return new ProgressInfo(sortedSongs, tasksInProgress);
//    }
    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/progresses2")
    public SimpleProgressInfo getProgressInfo2() {
        final List<TaskProgress> taskProgresses = progressService.getTasksInProgress2();
        final int numQueuedTasks = progressService.getNumQueuedTasks();
        return new SimpleProgressInfo(numQueuedTasks, taskProgresses);
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

    private Map<Integer, List<Song>> groupPerArtist(Collection<Integer> songIds) {
        final MutableInt notFound = new MutableInt(0);
        final Map<Integer, List<Song>> songsPerArtist =
                songIds.stream()
                        .map(this::findSongById)
                        .filter(song -> {
                            final boolean found = (song != null);
                            if (!found) {
                                notFound.increment();
                            }
                            return found;
                        })
                        .collect(Collectors.groupingBy(o -> o.getArtist().getArtistId()));
        if (notFound.toInteger() > 0) {
            logger.warn("{} songs not found", notFound.toInteger());
        }
        return songsPerArtist;
    }

    private List<TorrentSongSource> getSources(Song song, List<String> requestedSourceIds) {
        final List<TorrentSongSource> existingSongSources = this.searchService.getSongSources(song);
        final List<TorrentSongSource> sourcesToDownload;
        if (existingSongSources != null) {
            sourcesToDownload = filter(existingSongSources, requestedSourceIds);
        } else {
            sourcesToDownload = Collections.emptyList();
        }
        return sourcesToDownload;
    }

    private Map<Song, List<TorrentSongSource>> prepareDownloadRequest(Map<Integer, List<String>> songsDownloadInfos, List<Song> songs) {
        final Map<Song, List<TorrentSongSource>> downloadRequest = new HashMap<>();
        for (Song song : songs) {
            // requested sources ids to download
            final List<TorrentSongSource> sourcesToDownload = getSources(song, songsDownloadInfos.get(song.getSongId()));
            downloadRequest.put(song, sourcesToDownload);
        }
        return downloadRequest;
    }


    private Artist getSongsArtist(List<Song> artistSongs) {
        if (artistSongs.isEmpty()) {
            logger.warn("no artist songs found");
            throw new AssertionError();
        }
        final Artist artist;
        artist = artistSongs.get(0).getArtist();
        return artist;
    }


    private Artist getArtistById(Integer artistId) {
        final String artistName = playlistSongsMapper.getArtist(artistId);

        final Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setArtistName(artistName);
        return artist;
    }

    private Map<Song, List<TorrentSongSource>> prepareDownloadRequestSingleTorrent(String torrentId, List<String> sourceIds) {
        final Map<Song, List<TorrentSongSource>> downloadRequest = new HashMap<>();

        final List<SongSourceDocument> songSourceDocuments = dataStorage.getSongSourcesByTorrent(torrentId);
        for (SongSourceDocument songSourceDocument : songSourceDocuments) {
            if (!sourceIds.contains(songSourceDocument.getSourceId())) {
                // source skipped
                continue;
            }

            final Integer songId = songSourceDocument.getSongId();
            final TorrentSongSource songSource = songSourceDocument.getSongSource();

            final Song song = findSongById(songId);

            if (!downloadRequest.containsKey(song)) {
                downloadRequest.put(song, new ArrayList<>());
            }
            downloadRequest.get(song).add(songSource);
        }
        return downloadRequest;
    }
}
