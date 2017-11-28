package ru.asm.api.rest;

import com.google.common.collect.Lists;
import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.asm.core.AppConfiguration;
import ru.asm.core.AppCoreService;
import ru.asm.core.dev.model.*;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.ddb.SongSourceDocument;
import ru.asm.core.dev.model.ddb.TorrentDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
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

    private static final String TASK_INDEX_ARTISTS = "index artists";
    private static final String SEARCH_SONGS = "search songs";
    private static final String TASK_DOWNLOAD_TORRENTS = "download torrents";


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




    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/index/artists")
    public List<ArtistInfoVO> getArtistsForIndex() {
        final Set<Artist> artists = appCoreService.getPlaylistArtists("nashe");

        final List<ArtistInfoVO> artistInfos = new ArrayList<>();
        for (Artist artist : artists) {
            final ArtistInfoVO artistInfo = new ArtistInfoVO();
            artistInfo.setArtist(artist);

            final ArtistResolveReport artistResolveReport = this.appCoreService.getArtistResolveReport(artist);

            // todo: add error info + num found torrents
            final OperationStatus resultStatus = (artistResolveReport != null)
                    ? artistResolveReport.getResultStatus()
                    : OperationStatus.unkwnown;
            artistInfo.setIndexingStatus(resultStatus);

            artistInfos.add(artistInfo);
        }

        artistInfos.sort(Comparator.comparing(o -> o.getArtist().getArtistName()));

        return artistInfos;
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
                    final Artist artist = appCoreService.getArtistById(artistId);

                    logger.info("[{}/{}] indexing...", complete, total);

                    // start concrete task
                    final TaskProgress taskProgress = progressService.taskStarted(
                            taskId,
                            String.format("indexing '%s' artist", artist.getArtistName()),
                            artist,
                            null,
                            TASK_INDEX_ARTISTS
                    );

                    this.appCoreService.indexArtist(artist, taskProgress);
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
    public List<SongInfoVO> getSongsForSearch() {
        final List<Song> songs = appCoreService.getPlaylistSongs("nashe");

        //noinspection UnnecessaryLocalVariable
        final List<SongInfoVO> songInfos = new ArrayList<>(Lists.transform(songs, s -> {
            assert (s != null);

            final ArtistResolveReport artistResolveReport = this.appCoreService.getArtistResolveReport(s.getArtist());
            final List<TorrentSongSource> songSources = this.appCoreService.getSongSources(s);
            final List<FileDocument> songDownloadedFiles = this.appCoreService.getDownloadedSongs(s);

            final SongInfoVO songInfo = new SongInfoVO();

            songInfo.setSong(s);
            songInfo.setNumFiles(songDownloadedFiles.size());
            songInfo.setNumSources(songSources.size());

            // determine status
            final OperationStatus resolveStatus = (artistResolveReport != null)
                    ? artistResolveReport.getResultStatus(songSources)
                    : OperationStatus.unkwnown;

            songInfo.setResolveStatus(resolveStatus);

            return songInfo;
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
                final Song song = appCoreService.getSongById(songId);
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

                    this.appCoreService.searchSong(song.getArtist(), song, taskProgress);
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
    public List<TorrentInfoVO> getTorrentsForDownload() {
        final List<Song> songs = appCoreService.getPlaylistSongs("nashe");

        final Map<String, TorrentInfoVO> torrentInfos = new HashMap<>();

        for (Song song : songs) {
            final List<TorrentSongSource> sources = appCoreService.getSongSources(song);

            for (TorrentSongSource source : sources) {
                final String torrentId = source.getIndexSong().getTorrentId();

                if (!torrentInfos.containsKey(torrentId)) {
                    final TorrentDocument torrent = appCoreService.getTorrentById(torrentId);

                    final TorrentInfoVO torrentInfo = new TorrentInfoVO();
                    torrentInfo.setTorrentId(torrentId);
                    torrentInfo.setTitle(torrent.getTorrentInfo().getTitle());
                    torrentInfo.setFormat(torrent.getFormat());

                    torrentInfos.put(torrentId, torrentInfo);
                }

                torrentInfos.get(torrentId).getContainedSongs().add(song);
                torrentInfos.get(torrentId).getContainedSources().add(source);
            }
        }

        final List<TorrentInfoVO> result = new ArrayList<>(torrentInfos.values());
        result.sort(Comparator.comparing(TorrentInfoVO::getTitle));

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
                    this.appCoreService.downloadTorrent(torrentId, downloadRequest, taskProgress);
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
    public List<FileInfoVO> getDownloadedFiles() {
        final List<Song> songs = appCoreService.getPlaylistSongs("nashe");

        final List<FileInfoVO> fileInfos = new ArrayList<>();

        for (Song song : songs) {
            final List<FileDocument> files = appCoreService.getFilesBySongId(song.getSongId());

            for (FileDocument file : files) {
                final SongSourceDocument songSource = appCoreService.getSourceById(file.getSourceId());

                final FileInfoVO fileInfo = new FileInfoVO();
                fileInfo.setSong(song);
                fileInfo.setFile(file);
                fileInfo.setSongSource(songSource.getSongSource());

                fileInfos.add(fileInfo);
            }
        }

        fileInfos.sort(Comparator.comparing(o -> o.getSong().getFullName()));

        return fileInfos;
    }

    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/alt/build/files/start")
    public void buildPlaylist(List<Long> fileIds) {
        final List<FileInfoVO> downloadedFiles = getDownloadedFiles();
        final List<FileInfoVO> requestedFiles = downloadedFiles.stream()
                .filter(liteFileInfo -> fileIds.contains(liteFileInfo.getFile().getId()))
                .collect(Collectors.toList());


        final File outputFolder = new File(AppConfiguration.OUTPUT_FOLDER);


        if (!outputFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outputFolder.mkdirs();
        }

        for (FileInfoVO requestedFile : requestedFiles) {
            final File songFile = new File(requestedFile.getFile().getFsLocation());
            final File destFile = new File(outputFolder, songFile.getName());
            try {
                Files.copy(songFile.toPath(), destFile.toPath(), REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }



    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/progresses2")
    public SimpleProgressInfo getProgressInfo2() {
        final List<TaskProgress> taskProgresses = progressService.getTasksInProgress2();
        final int numQueuedTasks = progressService.getNumQueuedTasks();
        return new SimpleProgressInfo(numQueuedTasks, taskProgresses);
    }


    private Map<Song, List<TorrentSongSource>> prepareDownloadRequestSingleTorrent(String torrentId, List<String> sourceIds) {
        final Map<Song, List<TorrentSongSource>> downloadRequest = new HashMap<>();

        final List<SongSourceDocument> songSourceDocuments = appCoreService.getSongSourcesByTorrentId(torrentId);
        for (SongSourceDocument songSourceDocument : songSourceDocuments) {
            if (!sourceIds.contains(songSourceDocument.getSourceId())) {
                // source skipped
                continue;
            }

            final Integer songId = songSourceDocument.getSongId();
            final TorrentSongSource songSource = songSourceDocument.getSongSource();

            final Song song = appCoreService.getSongById(songId);

            if (!downloadRequest.containsKey(song)) {
                downloadRequest.put(song, new ArrayList<>());
            }
            downloadRequest.get(song).add(songSource);
        }
        return downloadRequest;
    }
}
