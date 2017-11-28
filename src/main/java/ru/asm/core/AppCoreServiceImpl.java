package ru.asm.core;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.asm.core.dev.model.Artist;
import ru.asm.core.dev.model.ArtistResolveReport;
import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.ddb.*;
import ru.asm.core.dev.model.torrent.ArtistTorrentsNotFoundException;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.flac.CueParser;
import ru.asm.core.flac.FFileDescriptor;
import ru.asm.core.flac.FTrackDescriptor;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.index.repositories.TorrentSongRepository;
import ru.asm.core.persistence.domain.ArtistEntity;
import ru.asm.core.persistence.domain.PlaylistSongEntity;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.progress.TaskProgress;
import ru.asm.core.torrent.TorrentClient;
import ru.asm.core.ttdb.TorrentsDbParser;
import ru.asm.util.ElasticUtils;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: artem.smirnov
 * Date: 28.08.2017
 * Time: 10:53
 */
@Component("appCoreService")
public class AppCoreServiceImpl implements AppCoreService {

    private static final Map<String, String> forumFormats = new HashMap<>();
    static {
        forumFormats.put("737", MusicFormats.FORMAT_FLAC); // Рок, Панк, Альтернатива (lossless)
        forumFormats.put("738", MusicFormats.FORMAT_MP3); // Рок, Панк, Альтернатива (lossy)
    }

    private static final Logger logger = LoggerFactory.getLogger(AppCoreServiceImpl.class);

    private static final int GROUP_SIZE = 10000;


    @Autowired
    private Node node;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private TorrentInfoRepository torrentInfoRepository;
    @Autowired
    private TorrentSongRepository torrentSongRepository;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private PlaylistSongsMapper playlistSongsMapper;

    @Autowired
    private DataStorage dataStorage;


    private TorrentClient torrentClient;


    @PostConstruct
    public void postConstruct() {
        logger.info("initializing...");

        // await at least for yellow status
        final ClusterHealthResponse response = elasticsearchOperations.getClient()
                .admin()
                .cluster()
                .prepareHealth()
                .setWaitForGreenStatus()
                .get();
        if (response.getStatus() != ClusterHealthStatus.YELLOW) {
            throw new IllegalStateException("repository is not initialized");
        }
        logger.info("elasticsearch initialized");

        torrentClient = new TorrentClient();
        torrentClient.initializeSession();
        logger.info("torrent client initialized");

        logger.info("initialization complete");
    }

    @PreDestroy
    public void preDestroy() {
        logger.info("destroying...");
        node.close();
        torrentClient.destroySession();
        logger.info("destroying complete");
    }

    @Override
    public void indexArtist(Artist artist, TaskProgress taskProgress) {
        final ArtistResolveReport artistResolveReport = new ArtistResolveReport(artist.getArtistId());
        artistResolveReport.setStartTime(new Date());

        // 1. resolve artist torrents stage
        taskProgress.log("resolving artist %s torrents...", artist.getArtistName());
        final ArtistDocument artistDocument = resolveArtistTorrents(artist, taskProgress, artistResolveReport);

        if (artistResolveReport.isResolveSucceeded()) {
            // 2. index artist torrents
            taskProgress.log("indexing artist %s torrents...", artist.getArtistName());
            indexArtistTorrents(artistDocument, taskProgress, artistResolveReport);
        } else {
            // resolve failed => skip indexing and search
            logger.info("indexing and search will be skipped due to resolve wasn't succeeded");
        }

        artistResolveReport.setEndTime(new Date());
        dataStorage.saveArtistResolveReport(artistResolveReport);
    }

    @Override
    public void searchSong(Artist artist, Song song, TaskProgress taskProgress) {
        final ArtistDocument artistDocument = dataStorage.getArtist(artist.getArtistId());

        if (artistDocument != null) {
            final List<TorrentDocument> indexedTorrents = artistDocument.getArtistTorrentIds().stream()
                    .map(s -> dataStorage.getTorrent(s))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!indexedTorrents.isEmpty()) {
                // 3. search sources (in indexed torrents)
                taskProgress.log("searching songs sources in index...");
                final List<TorrentSongSource> songSources = searchSongSources(artist, Collections.singletonList(song), indexedTorrents);
                logger.info("{} sources found", songSources.size());
            } else {
                logger.warn("no indexed torrents found for artist {}", artist.getArtistName());
            }
        } else {
            logger.warn("artist {} not indexed", artist.getArtistName());
        }
    }

    @Override
    public void downloadTorrent(String torrentId, Map<Song, List<TorrentSongSource>> downloadRequest, TaskProgress taskProgress) {
        // torrentId -> song-source[]
        final Map<String, List<SongFileDocument>> torrentSources = groupPerTorrent(downloadRequest);

        final Song firstSong = downloadRequest.keySet().iterator().next();
        final Artist artist = firstSong.getArtist();

        final long tId = taskProgress.startSubTask("download torrent...");
        try {
            final List<SongFileDocument> requestedSongs = torrentSources.get(torrentId);
            if (requestedSongs.isEmpty()) {
                logger.warn("no sources found");
                return;
            }
            final List<SongFileDocument> downloadedSongs;
            if (requestedSongs.get(0).getSongSource().isMp3()) {
                final String torrentName = artist.getArtistName();
                downloadedSongs = new Mp3FilesSupplier(dataStorage, torrentClient).downloadTorrent(torrentId, torrentName, requestedSongs, taskProgress);
            } else {
                downloadedSongs = new FlacFilesSupplier(dataStorage, torrentClient).downloadTorrent(torrentId, artist.getArtistName(), requestedSongs, taskProgress);
            }
            for (SongFileDocument downloadedSong : downloadedSongs) {
                logger.info("[{}] {} downloaded", downloadedSong.getSongSource().getSourceId(), downloadedSong.getFileDocument().getFsLocation());
            }
        } finally {
            taskProgress.completeSubTask(tId);
        }
    }

    @Override
    public List<FileDocument> getDownloadedSongs(Song song) {
        final List<FileDocument> files = dataStorage.getFiles(song.getSongId());
        final TreeSet<FileDocument> uniqueSet = new TreeSet<>(Comparator.comparing(FileDocument::getFsLocation));
        uniqueSet.addAll(files);
        return new ArrayList<>(uniqueSet);
    }

    @Override
    public List<TorrentSongSource> getSongSources(Song song) {
        return getSongSourcesFromStorage(song);
    }

    @Override
    public ArtistResolveReport getArtistResolveReport(Artist artist) {
        return dataStorage.getSongResolveReport(artist.getArtistId());
    }

    @Override
    public SongSourceDocument getSourceById(String sourceId) {
        return dataStorage.getSongSource(sourceId);
    }

    @Override
    public List<FileDocument> getFilesBySongId(Integer songId) {
        return dataStorage.getFiles(songId);
    }

    @Override
    public List<SongSourceDocument> getSongSourcesByTorrentId(String torrentId) {
        return dataStorage.getSongSourcesByTorrent(torrentId);
    }

    @Override
    public TorrentDocument getTorrentById(String torrentId) {
        return dataStorage.getTorrent(torrentId);
    }

    @Override
    public Song getSongById(Integer songId) {
        final Song song;
        PlaylistSongEntity foundSong = getSongEntityById(songId);
        if (foundSong != null) {
            song = convertToSong(foundSong);
        } else {
            song = null;
        }
        return song;
    }

    @Override
    public Artist getArtistById(Integer artistId) {
        final String artistName = playlistSongsMapper.getArtistNameById(artistId);

        final Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setArtistName(artistName);
        return artist;
    }

    @Override
    public Set<Artist> getPlaylistArtists(String playlistId) {
        final List<Song> songs = getPlaylistSongs(playlistId);
        return songs.stream()
                .map(Song::getArtist)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Song> getPlaylistSongs(@SuppressWarnings("SameParameterValue") String playlistId) {
        final Set<Song> songs = new HashSet<>();
        final List<PlaylistSongEntity> songEntities = this.playlistSongsMapper.getSongsByPlaylist(playlistId);
        for (PlaylistSongEntity songEntity : songEntities) {
            final Song song = convertToSong(songEntity);
            songs.add(song);
        }
        return new ArrayList<>(songs);
    }

    @Override
    public void downloadTorrent(File folder, String magnet) {
        if (!folder.exists()) //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();

        try {
            TorrentInfo torrentInfo = getByMagnet(magnet);

            String name = torrentInfo.name();
            File resumeFile = new File(folder, name + ".tmp");
            if (!resumeFile.exists()) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    resumeFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                }
            }

            torrentClient.download(torrentInfo, folder, null, null);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            logger.debug("download done");
        }
    }

    @Override
    @Transactional
    public void importPlaylist(String playlistName, String comment, File playlist) {
        List<SongDescriptor> songs = FileTools.readCsv(playlist);

        int i = 1;
        for (SongDescriptor song : songs) {
            final String artist = song.getArtist();
            final String songTitle = song.getTitle();

            if (artist != null && !artist.isEmpty() && songTitle != null) {
                Integer artistId = playlistSongsMapper.findArtistIdByName(artist);
                if (artistId == null) {
                    playlistSongsMapper.insertArtist(null, artist);
                    artistId = playlistSongsMapper.findArtistIdByName(artist);
                }
                assert (artistId != null);

                Integer songId = playlistSongsMapper.findSongIdByNameAndArtist(artistId, songTitle);
                if (songId == null) {
                    playlistSongsMapper.insertSong(null, artistId, songTitle);
                    songId = playlistSongsMapper.findSongIdByNameAndArtist(artistId, songTitle);
                }
                assert (songId != null);


                playlistSongsMapper.insertPlaylistSong(artistId, songId, playlistName, comment, i);
                logger.info("{}. [{}] '{}' into '{}' with comment: '{}'", i++, song.getArtist(), song.getTitle(), playlistName, comment);
            } else {
                logger.warn("{}. null", i++);
            }
        }
    }

    @Override
    public void indexTorrentsDb(File backup) {
        final InputStream inputStream;
        try {
            inputStream = new FileInputStream(backup);
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            return;
        }

        try {
            MutableLong totalIndexed = new MutableLong(0);
            Long count = torrentInfoRepository.count();
            logger.info(String.format("%d entries in repository", count));

            try {
                logger.info("start reading backup");
                TorrentsDbParser.parseDocument(inputStream, GROUP_SIZE, torrentInfos -> {
                    logger.info(String.format("%s more entries read", torrentInfos.size()));
                    torrentInfoRepository.save(torrentInfos);
                    totalIndexed.add(torrentInfos.size());
                    logger.info(String.format("%d entries added into index (total: %d)", torrentInfos.size(), totalIndexed.longValue()));
                });
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }

            count = torrentInfoRepository.count();
            logger.info(String.format("%d entries in repository", count));
        } finally {
            logger.info("closing opened resources");
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private TorrentInfo getByMagnet(String magnet) {
        return torrentClient.findByMagnet(magnet);
    }

    private PlaylistSongEntity getSongEntityById(Integer songId) {
        return this.playlistSongsMapper.getSongById(songId);
    }

    private List<TorrentSongSource> getSongSourcesFromStorage(Song song) {
        final List<SongSourceDocument> songSources = dataStorage.getSongSources(song.getSongId());
        return Lists.transform(songSources, SongSourceDocument::getSongSource);
    }


    private static Song convertToSong(PlaylistSongEntity foundSong) {
        final Song song = new Song();

        final Artist artist = new Artist();
        artist.setArtistId(foundSong.getArtistId());
        artist.setArtistName(foundSong.getArtist());

        song.setSongId(foundSong.getSongId());
        song.setTitle(foundSong.getTitle());
        song.setArtist(artist);
        return song;
    }

    private List<TorrentSongSource> searchSongSources(Artist artist, List<Song> songs, List<TorrentDocument> indexedTorrents) {
        final ArrayList<TorrentSongSource> result = new ArrayList<>();

        for (Song song : songs) {
            final String songTitle = song.getTitle();
            final String artistName = artist.getArtistName();

            logger.info("searching sources for {}: {}", artistName, songTitle);
            final List<TorrentSongSource> songSources = searchSongSourcesImpl(indexedTorrents, songTitle, artistName);
            saveSongSources(song, songSources);

            result.addAll(songSources);
        }

        return result;
    }

    @SuppressWarnings("UnusedReturnValue")
    private List<TorrentDocument> indexArtistTorrents(ArtistDocument artistDocument, TaskProgress taskProgress, ArtistResolveReport artistResolveReport) {
        final String artistName = artistDocument.getArtistName();

        final List<TorrentDocument> indexedTorrents = new ArrayList<>();
        logger.info("indexing artist {} torrents...", artistName);
        indexedTorrents.addAll(indexMp3Torrents(artistDocument, taskProgress));
        indexedTorrents.addAll(indexFlacTorrents(artistDocument, taskProgress));
        logger.info("indexing artist {} torrents complete", artistName);

        artistResolveReport.setIndexingPerformed(true);
        for (TorrentDocument indexedTorrent : indexedTorrents) {
            artistResolveReport.setTorrentIndexingStatus(indexedTorrent.getTorrentId(), indexedTorrent.getStatus());
        }

        return indexedTorrents;
    }

    private ArtistDocument resolveArtistTorrents(Artist artist,
                                                 TaskProgress taskProgress,
                                                 ArtistResolveReport report) {
        final Integer artistId = artist.getArtistId();
        final String artistName = artist.getArtistName();

        ArtistDocument artistDocument;
        try {
            artistDocument = dataStorage.getArtist(artistId);
            final boolean artistTorrentsAlreadyResolved = (artistDocument != null)
                    && (artistDocument.getArtistTorrentIds() != null)
                    && !artistDocument.getArtistTorrentIds().isEmpty();

            if (!artistTorrentsAlreadyResolved) {
                logger.info("resolving artist {} ({}) torrents", artistName, artistId);

                // start search artist torrents step
                taskProgress.log(String.format("resolving artist %s torrents...", artistName));

                // find artists torrents
                resolveArtistTorrentsImpl(artistId, artistName);
                logger.info("artist {} torrents resolved", artistName);
            } else {
                logger.info("artist {} torrents already resolved", artistName);
            }

            artistDocument = dataStorage.getArtist(artistId);
            assert (!artistDocument.getArtistTorrentIds().isEmpty());

            report.setResolvePerformed(true);
            report.setResolveStatus(ArtistResolveReport.Status.success);
            report.setResolvedTorrentIds(artistDocument.getArtistTorrentIds());
        } catch (ArtistTorrentsNotFoundException e) {
            report.setResolvePerformed(true);
            report.setResolveStatus(ArtistResolveReport.Status.failed);
            report.setResolveFailureReason(e.getMessage());
            artistDocument = null;
        }
        return artistDocument;
    }

    private List<TorrentSongSource> searchSongSourcesImpl(List<TorrentDocument> indexedTorrents, String songTitle, String artistName) {
        final List<TorrentSongSource> songSources = new ArrayList<>();

        for (TorrentDocument indexedTorrent : indexedTorrents) {
            final TorrentDocument torrentDocument = awaitFutureNoError(indexedTorrent);

            if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_OK)) {
                final String torrentId = torrentDocument.getTorrentId();

                final String[] songTerms = ElasticUtils.asTerms3(songTitle);
                logger.info(String.format("SEARCH %s: %s (%s) in torrent %s", artistName, songTitle, Arrays.asList(songTerms), torrentId));

                final Page<TorrentSongVO> indexSongs = findSongsInTorrentIndex(torrentId, songTerms);
                if (indexSongs.hasContent()) {
                    logger.info("{} found in torrent {}", indexSongs.getTotalElements(), torrentId);
                    for (TorrentSongVO indexSong : indexSongs) {
                        final TorrentSongSource songSource = new TorrentSongSource(indexSong);
                        songSources.add(songSource);
                    }
                } else {
                    logger.warn("nothing found in torrent {}", torrentId);
                }
            } else {
                logger.debug(torrentDocument.getStatus());
            }
        }
        return songSources;
    }

    private void resolveArtistTorrentsImpl(Integer artistId, String artistName)
            throws ArtistTorrentsNotFoundException {
        ArtistDocument artistDoc = dataStorage.getArtist(artistId);
        if (artistDoc == null) {
            artistDoc = createArtistDocument(artistId, artistName);
        }

        final String[] titleTerms = ElasticUtils.asTerms(artistName);
        logger.info(String.format("ARTIST: %s (%s)", artistName, Arrays.asList(titleTerms)));

        for (Map.Entry<String, String> entry : forumFormats.entrySet()) {
            final String forumId = entry.getKey();
            final String format = entry.getValue();

            final Page<TorrentInfoVO> page = findTorrentsByTitle(new String[]{forumId}, titleTerms);

            if (page.getNumberOfElements() > 0) {
                logger.info("{} torrents found for {} ({})", page.getNumberOfElements(), artistName, format);
                for (int i = 0; i < page.getNumberOfElements(); i++) {
                    final TorrentInfoVO ti = page.getContent().get(i);

                    final TorrentDocument torrentDocument = createTorrentDocument(ti, format);
                    dataStorage.updateTorrent(torrentDocument);

                    artistDoc.getArtistTorrentIds().add(torrentDocument.getTorrentId());
                }
            } else {
                logger.warn("no any torrents found for {}", artistName);
                throw new ArtistTorrentsNotFoundException(String.format("no any torrents found for artist %s", artistName));
            }
        }

        if (artistDoc.getArtistTorrentIds().isEmpty()) {
            logger.warn("no torrents found for artist {} ({})", artistName, artistId);
        }

        dataStorage.updateArtist(artistDoc);
    }

    private ArtistDocument createArtistDocument(Integer artistId, String artistName) {
        ArtistDocument artistDoc;
        artistDoc = new ArtistDocument();
        artistDoc.setArtistId(artistId);
        artistDoc.setArtistName(artistName);
        artistDoc.setArtistTorrentIds(new ArrayList<>());
        return artistDoc;
    }

    private TorrentDocument createTorrentDocument(TorrentInfoVO ti, String format) {
        final TorrentDocument torrentDocument = new TorrentDocument();
        torrentDocument.setTorrentId(ti.getId());
        torrentDocument.setFormat(format);
        torrentDocument.setStatus(ResolveStatuses.STATUS_UNKNOWN);
        torrentDocument.setTorrentInfo(ti);
        return torrentDocument;
    }

    private Page<TorrentInfoVO> findTorrentsByTitle(String[] forumQueries, String[] titleTerms) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (forumQueries != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("forumId", ElasticUtils.toLowerAll(forumQueries)));
        }
        if (titleTerms != null) {
            final BoolQueryBuilder builder = QueryBuilders.boolQuery();
            for (String term : titleTerms) {
                builder.should(QueryBuilders.wildcardQuery("title", term));
            }
            builder.minimumNumberShouldMatch(titleTerms.length);
            boolQueryBuilder.must(builder);
        }
        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(0, 999));

        return torrentInfoRepository.search(searchQueryBuilder.build());
    }


    private List<TorrentDocument> indexFlacTorrents(ArtistDocument artistDocument, TaskProgress taskProgress) {
        logger.info("indexing FLAC torrents...");

        final List<TorrentDocument> indexedTorrents = new ArrayList<>();

        // temp directory for FLAC mappings
        File saveDir = new File(AppConfiguration.FLAC_DOWNLOAD_TEMP_DIR);
        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
            saveDir.mkdirs();

        final ArtistEntity artistEntity = new ArtistEntity(artistDocument.getArtistId(), artistDocument.getArtistName());
        logger.info("{} torrents found", artistDocument.getArtistTorrentIds().size(), artistDocument.getArtistName());

        Integer total = artistDocument.getArtistTorrentIds().size();
        Integer completed = 0;

        final long tId = taskProgress.startSubTask("indexing FLAC torrents...");
        try {
            for (String torrentId : artistDocument.getArtistTorrentIds()) {
                taskProgress.setSubTaskProgress(tId, completed, total);

                logger.info("indexing {} torrent...", torrentId);

                final TorrentDocument torrentDocument = dataStorage.getTorrent(torrentId);
                final TorrentInfoVO dumpTorrentInfo = torrentDocument.getTorrentInfo();

                if (!torrentDocument.getFormat().equals(MusicFormats.FORMAT_FLAC)) {
                    logger.info("skipping torrent {} due to it's not FLAC torrent", torrentId);
                    completed++;
                    continue;
                }

                if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_UNKNOWN)
                        || torrentDocument.getStatus().equals(ResolveStatuses.STATUS_ERROR)) {

                    Boolean error = false;
                    final File torrentDir = new File(saveDir, getFolderForTorrent(dumpTorrentInfo));

                    final Set<TorrentSongVO> torrentSongs = new HashSet<>();

                    //noinspection ConstantConditions
                    if (!isAlreadyDownloaded(torrentDir)) {

                        // find torrent info by magnet
                        taskProgress.log("resolving magnet (%s/%s)...", completed + 1, total);
                        logger.info("fetching torrent info by magnet...");
                        final TorrentInfo torrentInfo = getByMagnet(dumpTorrentInfo.getMagnet());
                        logger.info("fetch completed");

                        if (torrentInfo != null) {
                            taskProgress.log("downloading files (%s/%s)...", completed + 1, total);

                            // check legacy directory (probably already downloaded but folder has deprecated name)
                            final File legacyTorrentDir = new File(saveDir, getLegacyFolderForTorrent(torrentInfo));

                            //noinspection ConstantConditions
                            if (legacyTorrentDir.exists() && legacyTorrentDir.isDirectory() && legacyTorrentDir.list().length > 0) {
                                // if legacy directory exists - just rename it in proper way
                                if (!legacyTorrentDir.renameTo(torrentDir)) {
                                    throw new AssertionError();
                                }
                            } else if (isDownloadAllowed()) {
                                // need download cue files

                                // set priority for .cue files
                                final List<String> cueFilePaths = new ArrayList<>();
                                final Priority[] priorities = ignoreAllExceptCuePriorities(torrentInfo, cueFilePaths);
                                // download cue files
                                logger.info("downloading cue file...");
                                downloadCue(torrentInfo, priorities, torrentDir, taskProgress);
                                logger.info("download complete");
                            } else {
                                error = true;
                            }

                        } else {
                            logger.debug("fetch torrent info has failed");
                            error = true;
                        }
                    } else {
                        logger.info("torrent {} is already downloaded", torrentId);
                    }

                    // already downloaded, parse downloaded cue files
                    if (torrentDir.exists()) {
                        torrentSongs.addAll(getTorrentSongs(torrentId, torrentDir, artistEntity));
                        logger.info("{} songs found", torrentSongs.size());
                    } else {
                        error = true;
                    }

                    if (!torrentSongs.isEmpty()) {
                        // index torrent songs
                        torrentSongRepository.save(torrentSongs);
                    } else {
                        logger.warn("not found or timeout expired");
                    }

                    if (!error) {
                        torrentDocument.setStatus(ResolveStatuses.STATUS_OK);
                    } else {
                        torrentDocument.setStatus(ResolveStatuses.STATUS_ERROR);
                    }

                    dataStorage.updateTorrent(torrentDocument);

                    indexedTorrents.add(torrentDocument);
                } else {
                    logger.info("torrent {} is already indexed", torrentId);
                    indexedTorrents.add(torrentDocument);
                }

                completed++;
            }
        } finally {
            taskProgress.completeSubTask(tId);
        }

        return indexedTorrents;
    }


    private List<TorrentDocument> indexMp3Torrents(ArtistDocument artistDocument, TaskProgress taskProgress) {
        final String artistName = artistDocument.getArtistName();
        final List<String> artistTorrentIds = artistDocument.getArtistTorrentIds();

        logger.info("indexing MP3 torrents...");
        logger.info("{} torrents found", artistTorrentIds.size(), artistName);
        taskProgress.log(String.format("indexing artist %s MP3 torrents...", artistName));

        final List<TorrentDocument> indexedTorrents = new ArrayList<>();

        Integer total = artistTorrentIds.size();
        Integer complete = 0;

        final long tId = taskProgress.startSubTask("indexing MP3 torrents...");
        try {
            for (String torrentId : artistTorrentIds) {
                taskProgress.setSubTaskProgress(tId, complete, total);

                final TorrentDocument torrentDocument = dataStorage.getTorrent(torrentId);

                if (!torrentDocument.getFormat().equals(MusicFormats.FORMAT_MP3)) {
                    logger.debug("skipping torrent {} due to it's not MP3 torrent", torrentId);
                    complete++;
                    continue;
                }

                if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_UNKNOWN)
                        || torrentDocument.getStatus().equals(ResolveStatuses.STATUS_ERROR)) {
                    logger.info("indexing torrent {} (previous status {})", torrentId, torrentDocument.getStatus());

                    taskProgress.log("resolving magnet (%s/%s)...", complete + 1, total);
                    logger.info("fetching torrent {} info by magnet {}", torrentId, torrentDocument.getTorrentInfo().getMagnet());
                    final TorrentInfo torrentInfo = getByMagnet(torrentDocument.getTorrentInfo().getMagnet());
                    if (torrentInfo != null) {
                        logger.info("torrent {} info fetched", torrentId);
                    } else {
                        logger.warn("torrent {} info not found or timeout expired", torrentId);
                    }

                    if (torrentInfo == null) {
                        // not found => error
                        torrentDocument.setStatus(ResolveStatuses.STATUS_ERROR);
                    } else {
                        // found
                        final Set<TorrentSongVO> torrentSongs = getTorrentSongsMp3(torrentId, torrentInfo, artistName);
                        torrentDocument.setStatus(ResolveStatuses.STATUS_OK);

                        // index torrent songs
                        if (!torrentSongs.isEmpty()) {
                            logger.info("{} songs indexed in torrent {}", torrentSongs.size(), torrentId);
                            torrentSongRepository.save(torrentSongs);
                        } else {
                            logger.info("no songs indexed in torrent {}", torrentId);
                        }
                    }

                    // update torrent information
                    dataStorage.updateTorrent(torrentDocument);

                    indexedTorrents.add(torrentDocument);
                } else {
                    logger.info("torrent {} is already indexed", torrentId);
                    indexedTorrents.add(torrentDocument);
                }

                complete++;
            }
        } finally {
            taskProgress.completeSubTask(tId);
        }

        return indexedTorrents;
    }

    private TorrentDocument awaitFutureNoError(TorrentDocument indexedTorrent) {
        return indexedTorrent;
    }

    private Page<TorrentSongVO> findSongsInTorrentIndex(String torrentId, String[] songTerms) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (songTerms != null) {
            final BoolQueryBuilder builder = QueryBuilders.boolQuery();
            for (String term : songTerms) {
                builder.should(QueryBuilders.wildcardQuery("songName", term));
            }
            builder.minimumNumberShouldMatch(songTerms.length > 2 ? songTerms.length - 1 : songTerms.length);

            boolQueryBuilder.must(builder);
        }

        if (torrentId != null) {
            boolQueryBuilder.must(QueryBuilders.termQuery("torrentId", torrentId));
        }

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(0, 999));

        return torrentSongRepository.search(searchQueryBuilder.build());
    }

    private Set<TorrentSongVO> getTorrentSongsMp3(String torrentId, TorrentInfo torrentInfo, String artistName) {
        final Set<TorrentSongVO> torrentSongs = new HashSet<>();
        for (int j = 0; j < torrentInfo.files().numFiles(); j++) {
            final FileStorage fileStorage = torrentInfo.files();
            final String filePath = fileStorage.filePath(j);
            final String fileName = fileStorage.fileName(j);

            if (fileName.toLowerCase().endsWith(".mp3")) {
                final String title = fileName.replace(".mp3", "").replace(".MP3", "");
                torrentSongs.add(new TorrentSongVO(torrentId, title, artistName, fileName, filePath));
            }
        }
        return torrentSongs;
    }


    private Set<TorrentSongVO> getTorrentSongs(String torrentId, File torrentDir, ArtistEntity artistEntity) {
        final List<FFileDescriptor> fFileDescriptors = new ArrayList<>();
        final Iterator<File> fileIterator = FileUtils.iterateFiles(torrentDir, new String[]{"cue"}, true);
        while (fileIterator.hasNext()) {
            final File cueFile = fileIterator.next();
            try {
                fFileDescriptors.addAll(new CueParser().parseCue(torrentDir, cueFile));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        final Set<TorrentSongVO> torrentSongs = new HashSet<>();
        for (FFileDescriptor fFileDescriptor : fFileDescriptors) {
            for (FTrackDescriptor fTrackDescriptor : fFileDescriptor.getTrackDescriptors()) {
                torrentSongs.add(new TorrentSongVO(torrentId, fTrackDescriptor.getTitle(), artistEntity.getArtist(),
                        fFileDescriptor.getRelativePath(), fFileDescriptor.getRelativePath(),
                        fTrackDescriptor.getTrackNum(), fTrackDescriptor.getIndexTime()));
            }
        }
        return torrentSongs;
    }

    @SuppressWarnings("UnusedReturnValue")
    private File downloadCue(TorrentInfo torrentInfo, Priority[] priorities, File targetFolder, TaskProgress taskProgress) {
        if (!targetFolder.exists()) //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
        getTorrentClient().download(torrentInfo, targetFolder, priorities, taskProgress);
        return targetFolder;
    }

    private Priority[] ignoreAllExceptCuePriorities(TorrentInfo torrentInfo, List<String> cueFilePaths) {
        // enlist files
        final int numFiles = torrentInfo.files().numFiles();
        final Priority[] priorities = TorrentUtils.getPrioritiesIgnoreAll(numFiles);
        for (int j = 0; j < numFiles; j++) {
            final FileStorage fileStorage = torrentInfo.files();
            final String filePath = fileStorage.filePath(j);
            /*final long fileSize = fileStorage.fileSize(j);*/
            final String fileName = fileStorage.fileName(j);

//            logger.info("{} ({})", filePath, fileSize);

            if (fileName.endsWith(".cue")) {
                priorities[j] = Priority.NORMAL;
                cueFilePaths.add(filePath);
            }
        }
        return priorities;
    }

    private boolean isDownloadAllowed() {
        return AppConfiguration.DOWNLOAD_ALLOWED;
    }

    /**
     * leave it only to avoid downloading already downloaded files
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    private static String getLegacyFolderForTorrent(TorrentInfo torrentInfo) {
        return torrentInfo.name();
    }

    private boolean isAlreadyDownloaded(File torrentDir) {
        //noinspection ConstantConditions
        return torrentDir.exists() && torrentDir.list().length > 0;
    }

    private static String getFolderForTorrent(TorrentInfoVO dumpTorrentInfo) {
        return dumpTorrentInfo.getId();
    }



    private void saveSongSources(Song song, List<TorrentSongSource> songSources) {
        for (TorrentSongSource songSource : songSources) {
            final SongSourceDocument existing = dataStorage.getSongSource(songSource.getSourceId());

            if (existing == null) {
                final SongSourceDocument songSourceDocument = new SongSourceDocument();
                songSourceDocument.setSongId(song.getSongId());
                songSourceDocument.setSourceId(songSource.getSourceId());
                songSourceDocument.setSongSource(songSource);

                dataStorage.updateSongSource(songSourceDocument);
            }
        }
    }

    private Map<String, List<SongFileDocument>> groupPerTorrent(Map<Song, List<TorrentSongSource>> downloadRequest) {
        final Map<String, List<SongFileDocument>> torrentSources = new HashMap<>();
        for (Map.Entry<Song, List<TorrentSongSource>> entry : downloadRequest.entrySet()) {
            for (TorrentSongSource source : entry.getValue()) {
                final String torrentId = source.getIndexSong().getTorrentId();
                if (!torrentSources.containsKey(torrentId)) {
                    torrentSources.put(torrentId, new ArrayList<>());
                }
                torrentSources.get(torrentId).add(new SongFileDocument(entry.getKey(), source, null));
            }
        }
        return torrentSources;
    }

    private TorrentClient getTorrentClient() {
        return torrentClient;
    }

}
