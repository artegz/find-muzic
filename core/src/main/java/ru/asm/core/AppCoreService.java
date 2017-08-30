package ru.asm.core;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.asm.core.flac.FFileDescriptor;
import ru.asm.core.flac.FTrackDescriptor;
import ru.asm.core.index.TorrentsDatabaseService;
import ru.asm.core.index.domain.TorrentFilesVO;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentFilesRepository;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.index.repositories.TorrentSongRepository;
import ru.asm.core.persistence.domain.ArtistEntity;
import ru.asm.core.persistence.domain.PlaylistSongEntity;
import ru.asm.core.persistence.domain.ResolvedSongEntity;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.torrent.TorrentClient;
import ru.asm.core.ttdb.TorrentsDbParser;
import ru.asm.tools.CueParser;
import ru.asm.tools.dev.SingleSongResult;
import ru.asm.util.ElasticUtils;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * User: artem.smirnov
 * Date: 28.08.2017
 * Time: 10:53
 */
@Component
public class AppCoreService {

    private static final Logger logger = LoggerFactory.getLogger(AppCoreService.class);

    private static final int GROUP_SIZE = 10000;
    private static final int PARALLELISM = 8;
    private static final boolean DOWNLOAD_ALLOWED = true;
    private static final List<String> OVERRIDE_STATUSES = Arrays.asList(ResolveStatuses.STATUS_UNKNOWN, ResolveStatuses.STATUS_ERROR);


    @Autowired
    private Node node;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private TorrentInfoRepository torrentInfoRepository;

    @Autowired
    private TorrentFilesRepository torrentFilesRepository;

    @Autowired
    private TorrentSongRepository torrentSongRepository;

    @Autowired
    private PlaylistSongsMapper playlistSongsMapper;


    private TorrentClient torrentClient;
    private ForkJoinPool forkJoinPool;
    private ExecutorService executorService;


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

        forkJoinPool = new ForkJoinPool(PARALLELISM);
        executorService = Executors.newFixedThreadPool(PARALLELISM);
        logger.info("fork join pool initialized");

        logger.info("initialization complete");
    }

    @PreDestroy
    public void preDestroy() {
        logger.info("destroying...");
        node.close();
        torrentClient.destroySession();
        logger.info("destroying complete");
    }

    public TorrentClient getTorrentClient() {
        return torrentClient;
    }

    public void downloadTorrent(File folder, String magnet) {
        if (!folder.exists()) folder.mkdirs();

        try {
            TorrentInfo torrentInfo = getByMagnet(magnet);

            String name = torrentInfo.name();
            File resumeFile = new File(folder, name + ".tmp");
            if (!resumeFile.exists()) {
                try {
                    resumeFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                }
            }

            torrentClient.download(torrentInfo, folder, null);
        } finally {
        }
    }

    @Transactional
    public void importPlaylist(String playlistName, String comment, File playlist) {
        List<SongDescriptor> songs = FileTools.readCsv(playlist);

        int i = 1;
        for (SongDescriptor song : songs) {
            final String artist = song.getArtist();
            final String songTitle = song.getTitle();

            if (artist != null && !artist.isEmpty() && songTitle != null && !artist.isEmpty()) {
                Integer artistId = playlistSongsMapper.findArtistId(artist);
                if (artistId == null) {
                    playlistSongsMapper.insertArtist(null, artist);
                    artistId = playlistSongsMapper.findArtistId(artist);
                }
                assert (artistId != null);

                Integer songId = playlistSongsMapper.findSongId(artistId, songTitle);
                if (songId == null) {
                    playlistSongsMapper.insertSong(null, artistId, songTitle);
                    songId = playlistSongsMapper.findSongId(artistId, songTitle);
                }
                assert (songId != null);


                playlistSongsMapper.insertPlaylistSong(artistId, songId, playlistName, comment);
                logger.info("{}. [{}] '{}' into '{}' with comment: '{}'", i++, song.getArtist(), song.getTitle(), playlistName, comment);
            } else {
                logger.warn("{}. null", i++);
            }
        }
    }

    public void indexTorrentsDb(File backup) {
        final InputStream inputStream;
        try {
            inputStream = new FileInputStream(backup);
        } catch (FileNotFoundException e) {
            logInfo(e.getMessage());
            return;
        }

        try {
            // await at least for yellow status
//            logInfo("initializing elasticsearch");
//            final ClusterHealthResponse response = elasticsearchOperations.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().get();
//            if (response.getStatus() != ClusterHealthStatus.YELLOW) {
//                throw new AssertionError();
//            }
//            logInfo("elasticsearch initialized");

            MutableLong totalIndexed = new MutableLong(0);
            Long count = torrentInfoRepository.count();
            logInfo("%d entries in repository", count);

            try {
                logInfo("start reading backup", count);
                TorrentsDbParser.parseDocument(inputStream, GROUP_SIZE, torrentInfos -> {
                    logInfo("%s more entries read", torrentInfos.size());
                    torrentInfoRepository.save(torrentInfos);
                    totalIndexed.add(torrentInfos.size());
                    logInfo("%d entries added into index (total: %d)", torrentInfos.size(), totalIndexed.longValue());
                });
            } catch (Throwable e) {
                e.printStackTrace();
                logInfo(e.getMessage());
            }

            count = torrentInfoRepository.count();
            logInfo("%d entries in repository", count);
        } finally {
            logInfo("closing opened resources");
            try {
                inputStream.close();
            } catch (IOException e) {
                logInfo(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public List<Integer> getArtistIds() {
        return playlistSongsMapper.getAllArtistsIds();
    }

    public List<ResolvedSongEntity> getFoundSongs() {
        final List<ResolvedSongEntity> foundSongs = playlistSongsMapper.getFoundSongs();

//        foundSongs.forEach(resolvedSongEntity -> {
//            NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();
//
//            final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
//
//            boolQueryBuilder.must(QueryBuilders.termQuery("torrentId", resolvedSongEntity.getTorrentId()));
//            boolQueryBuilder.must(QueryBuilders.termQuery("id", resolvedSongEntity.getFileId()));
//
//            searchQueryBuilder.withQuery(boolQueryBuilder);
//            searchQueryBuilder.withPageable(new PageRequest(0, 1));
//            searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC));
//
//            Page<TorrentSongVO> result = torrentSongRepository.search(searchQueryBuilder.build());
//
//            assert (result.hasContent());
//
//            final TorrentSongVO songVO = result.iterator().next();
//        });

        return foundSongs;
    }

    public void downloadFoundSongs_mp3(Integer offset, Integer limit) {
        List<ResolvedSongEntity> foundSongs = playlistSongsMapper.getFoundSongs();
        foundSongs = page(foundSongs, offset, limit);
        
        logger.info("downloading {} songs...", foundSongs.size());
        
        foundSongs.forEach(songEntity -> {
            final String torrentId = songEntity.getTorrentId();

            // find dump torrent info (with magnet)
            final TorrentInfoVO dumpTorrentInfo = findTorrent(torrentId);
            assert (dumpTorrentInfo != null);

            // find song file indexed entity
            final TorrentSongVO torrentSong = findTorrentSong(torrentId, songEntity.getFileId());
            assert (torrentSong != null);

            logger.info("[{}: {}] resolving magnet", torrentSong.getArtistName(), torrentSong.getSongName());
            getByMagnetAsync(dumpTorrentInfo.getMagnet())
                    .thenAcceptAsync(torrentInfo -> {
                        final String mp3FilePath = torrentSong.getMp3FilePath();

                        logger.info("[{}: {}] magnet resolved, downloading '{}'", torrentSong.getArtistName(), torrentSong.getSongName(), mp3FilePath);

                        final Priority[] priorities = getSingleFilePriorities(torrentInfo, mp3FilePath);

                        final File saveDir = new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE);
                        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                            saveDir.mkdirs();
                        torrentClient.download(torrentInfo, saveDir, priorities);

                        final File downloadedSong = new File(saveDir, mp3FilePath);
                        if (downloadedSong.exists()) {
                            playlistSongsMapper.insertDownloadedSong(songEntity.getSongId(), mp3FilePath);
                            logger.info("[{}: {}] download complete", torrentSong.getArtistName(), torrentSong.getSongName(), mp3FilePath);
                        } else {
                            logger.warn("[{}: {}] download failed", torrentSong.getArtistName(), torrentSong.getSongName(), mp3FilePath);
                        }
                    });

        });
    }

    @Transactional
    public void resolveArtists() {
        final List<String> found = new ArrayList<>();
        final List<String> notFound = new ArrayList<>();

        final Map<String, String> forumFormats = new HashMap<>();
        forumFormats.put("737", MusicFormats.FORMAT_FLAC); // Рок, Панк, Альтернатива (lossless)
        forumFormats.put("738", MusicFormats.FORMAT_MP3); // Рок, Панк, Альтернатива (lossy)

        final List<Integer> artistsIds = playlistSongsMapper.getAllArtistsIds();
        final TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(torrentInfoRepository);

        // prepare tasks to be executed
        for (Integer artistId : artistsIds) {
            final String artist = playlistSongsMapper.getArtist(artistId);
            final String[] titleTerms = ElasticUtils.asTerms(artist);

            logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));

            for (Map.Entry<String, String> entry : forumFormats.entrySet()) {
                final String forumId = entry.getKey();
                final String format = entry.getValue();

                final Page<TorrentInfoVO> page = torrentsDatabaseService.findPage(new String[] {forumId}, titleTerms, 0, 50);

                if (page.getNumberOfElements() > 0) {
                    found.add(artist);

                    for (int i = 0; i < page.getNumberOfElements(); i++) {
                        final TorrentInfoVO ti = page.getContent().get(i);
//                        playlistSongsMapper.insertArtistTorrent(artistId, ti.getId(), format, forumId, ResolveStatuses.STATUS_UNKNOWN);
                        playlistSongsMapper.insertTorrent(ti.getId(), format, forumId, ResolveStatuses.STATUS_UNKNOWN);
                        playlistSongsMapper.insertArtistTorrentLink(artistId, ti.getId());
                    }
                } else {
                    notFound.add(artist);
                }

                logger.info("total: {} {}", page.getTotalElements(), format);
            }

        }
    }

    @Transactional
    public void resolveSongs_mp3(Integer offset, Integer limit) {
        final List<TorrentFilesVO> torrentsToIndex = new ArrayList<>();
        final List<TorrentSongVO> songsToIndex = new ArrayList<>();

        final List<Integer> artistsIds = page(playlistSongsMapper.getAllArtistsIds(), offset, limit);
        final ResolveResult artistsTorrents = findResolvedArtistsTorrents(artistsIds);

        final Set<String> torrentIds = artistsTorrents.torrents.keySet();

        for (String torrentId : torrentIds) {
            final Future<TorrentInfo> torrentInfoFuture = artistsTorrents.torrents.get(torrentId);
            final TorrentInfoVO torrent = artistsTorrents.torrentInfos.get(torrentId);
            final ArtistEntity artistEntity = artistsTorrents.torrentArtists.get(torrentId);

            TorrentInfo torrentInfo = null;
            try {
                torrentInfo = torrentInfoFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("[{}] error occurred during retrieving torrent info: {}", torrentId, e.getMessage());
                logger.debug(e.getMessage(), e);
            }

            if (torrentInfo == null) {
                playlistSongsMapper.updateTorrentStatus(torrent.getId(), ResolveStatuses.STATUS_ERROR);
                continue;
            }

            // find all mp3 files in torrent
            final Set<TorrentSongVO> torrentSongs = getTorrentSongsMp3(torrent.getId(), torrentInfo, artistEntity);

            torrentsToIndex.add(TorrentFilesVO.create(torrent, artistEntity, torrentSongs));
            songsToIndex.addAll(torrentSongs);

            playlistSongsMapper.updateTorrentStatus(torrent.getId(), ResolveStatuses.STATUS_OK);

        }

        torrentFilesRepository.save(torrentsToIndex);
        torrentSongRepository.save(songsToIndex);

        logger.info("total: {}", torrentIds.size());


        logger.info("FOUND ({}): {}", artistsTorrents.found.size(), artistsTorrents.found);
        logger.warn("NOT FOUND ({}): {}", artistsTorrents.notFound.size(), artistsTorrents.notFound);
    }

    @Transactional
    public void resolveSongs_flac(Integer offset, Integer limit) {
        final TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(torrentInfoRepository);

        File saveDir = new File("C:\\TEMP\\find-music\\downloads\\flac\\");
        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
            saveDir.mkdirs();

        final String forumId = "737";

        // cleanup db log
//        playlistSongsMapper.deleteAllArtistTorrent(FORMAT_FLAC);
        final List<Integer> artistsIds = page(playlistSongsMapper.getAllArtistsIds(), offset, limit);

        try {
            final List<String> found = new ArrayList<>();
            final List<String> notFound = new ArrayList<>();

            {
                final List<ResolveSongsTask> tasks = new ArrayList<>();

                // prepare tasks to be executed
                for (Integer artistId : artistsIds) {
                    final String artist = playlistSongsMapper.getArtist(artistId);

                    final List<String> torrentIds = new ArrayList<>();
                    for (String overrideStatus : OVERRIDE_STATUSES) {
                        torrentIds.addAll(playlistSongsMapper.getArtistTorrents2(artistId, MusicFormats.FORMAT_FLAC, overrideStatus));
                    }

                    final String[] titleTerms = ElasticUtils.asTerms2(artist);

                    logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));

                    if (!torrentIds.isEmpty()) {
                        found.add(artist);
                        for (String torrentId : torrentIds) {
                            final TorrentInfoVO dumpTorrentInfo = findTorrent(torrentId);
                            if (dumpTorrentInfo == null) {
                                throw new AssertionError();
                            }

                            final ResolveSongsTask resolveSongsTask = new ResolveSongsTask(artist, artistId, dumpTorrentInfo, dumpTorrentInfo.getId(), () -> {
                                final File torrentDir = new File(saveDir, getFolderForTorrent(dumpTorrentInfo));

                                final Set<TorrentSongVO> torrentSongs = new HashSet<>();

                                //noinspection ConstantConditions
                                if (!isAlreadyDownloaded(torrentDir)) {

                                    // find torrent info by magnet
                                    final TorrentInfo torrentInfo = getByMagnet(dumpTorrentInfo.getMagnet());
                                    if (torrentInfo != null) {
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
                                            downloadCue(torrentInfo, priorities, torrentDir);
                                        }
                                    } else {
                                        logger.debug("fetch torrent info has failed");
                                    }
                                }

                                // already downloaded, parse downloaded cue files
                                if (torrentDir.exists()) {
                                    torrentSongs.addAll(getTorrentSongs(torrentId, torrentDir, new ArtistEntity(artistId, artist)));
                                }

                                if (!torrentSongs.isEmpty()) {
                                    final TorrentFilesVO torrentFilesVO = new TorrentFilesVO();
                                    torrentFilesVO.setTorrentId(dumpTorrentInfo.getId());
                                    torrentFilesVO.setArtist(artist);
                                    torrentFilesVO.setArtistId(artistId);
                                    torrentFilesVO.setForumId(forumId);
                                    torrentFilesVO.setMagnet(dumpTorrentInfo.getMagnet());
                                    torrentFilesVO.setTorrentSongs(new ArrayList<>(torrentSongs));

                                    return torrentFilesVO;
                                } else {
                                    logger.warn("not found or timeout expired");
                                    return null;
                                }
                            });
                            tasks.add(resolveSongsTask);
                        }
                    } else {
                        notFound.add(artist);
                    }

                    logger.info("total: {}", torrentIds.size());
                }

                // submit all tasks
                for (ResolveSongsTask task : tasks) {
                    forkJoinPool.submit(task);
                }

                // join all tasks
                int i = 0;
                final int total = tasks.size();
                for (ResolveSongsTask task : tasks) {
                    i++;
                    final TorrentFilesVO taskResult = task.join();
                    if (taskResult != null) {
                        logger.info("[{}/{}] task completed", i, total);

                        torrentFilesRepository.index(taskResult);
                        torrentSongRepository.save(taskResult.getTorrentSongs());

//                        playlistSongsMapper.updateArtistTorrentStatus(task.getArtistId(), MusicFormats.FORMAT_FLAC, task.getTorrentId(), ResolveStatuses.STATUS_OK);
                        playlistSongsMapper.updateTorrentStatus(task.getTorrentId(), ResolveStatuses.STATUS_OK);
                    } else {
                        final Throwable e = task.getError();
                        if (e != null) {
                            logger.error(e.getMessage());
                        }
                        logger.warn("[{}/{}] task failed", i, total);
//                        playlistSongsMapper.updateArtistTorrentStatus(task.getArtistId(), MusicFormats.FORMAT_FLAC, task.getTorrentId(), ResolveStatuses.STATUS_ERROR);
                        playlistSongsMapper.updateTorrentStatus(task.getTorrentId(), ResolveStatuses.STATUS_ERROR);
                    }
                }
            }

            logger.info("FOUND ({}): {}", found.size(), found);
            logger.warn("NOT FOUND ({}): {}", notFound.size(), notFound);
        } finally {

        }
    }

    @Transactional
    public SongsSearchResult resolveSongs() {
        final List<PlaylistSongEntity> songs = playlistSongsMapper.getSongs();
//        for (PlaylistSongEntity song : songs) {
//            System.out.println(song);
//        }

        final SongsSearchResult songsSearchResult = new SongsSearchResult();
        try {
            for (PlaylistSongEntity song : songs) {

                final String[] artistTerms = ElasticUtils.asTerms3(song.getArtist());
                final String[] songTerms = ElasticUtils.asTerms3(song.getTitle());

                logger.info(String.format("SEARCH %s (%s): %s (%s)", song.getArtist(), Arrays.asList(artistTerms), song.getTitle(), Arrays.asList(songTerms)));

                // find torrents with song
                Page<TorrentFilesVO> page = findPage(null, artistTerms, songTerms, 0, 10);
                if (page.getNumberOfElements() > 0) {
                    songsSearchResult.found.add(new SingleSongResult(song));
                    page.forEach(t -> {
                        // find song within torrent
                        final Page<TorrentSongVO> indexSongs = findSongs(t.getTorrentId(), songTerms, 0, 10);
                        if (indexSongs.hasContent()) {
                            final TorrentSongVO songVO = indexSongs.iterator().next();
                            playlistSongsMapper.insertSongTorrentLink(song.getSongId(), song.getArtistId(), t.getTorrentId(), songVO.getId());
                        }
                    });

                    logger.info("{} torrents found", page.getTotalElements());

                    {
                        final List<TorrentFilesVO> content = page.getContent();
                        for (TorrentFilesVO torrentFilesVO : content) {
                            logger.info("- {} ({} files)", torrentFilesVO.getTorrentId(), torrentFilesVO.getTorrentSongs().size());
//                                for (TorrentSongVO torrentSongVO : torrentFilesVO.getTorrentSongsMp3()) {
//                                    logger.info(" - " + torrentSongVO.getSongName());
//                                }
//
//                                for (String file : torrentFilesVO.getFileNames()) {
//                                    logger.info(" - " + file);
//                                }
                        }
                        //break;
                    }
                } else {
                    songsSearchResult.notFound.add(new SingleSongResult(song));
                    logger.warn("not found");
                }
            }

//            for (String s : notFound) {
//                System.out.println("\"" + s + "\"");
//            }
        } finally {
        } return songsSearchResult;
    }

    private TorrentSongVO getFoundTorrentSong(List<TorrentSongVO> torrentSongs) {
        return torrentSongs.get(0);
    }

    public Page<TorrentSongVO> findSongs(String torrentId, String[] songTerms, int page, int pageSize) {
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
        searchQueryBuilder.withPageable(new PageRequest(page, pageSize));
//        searchQueryBuilder.withSort(new FieldSortBuilder("artist").order(SortOrder.ASC));

        Page<TorrentSongVO> result = torrentSongRepository.search(searchQueryBuilder.build());

        return result;
    }

    public Page<TorrentFilesVO> findPage(String[] forumIdTerms, String[] artistTerms, String[] songTerms, int page, int pageSize) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (forumIdTerms != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("forumId", forumIdTerms));
        }
        if (artistTerms != null) {
            final BoolQueryBuilder builder = QueryBuilders.boolQuery();
            for (String term : artistTerms) {
                builder.should(QueryBuilders.wildcardQuery("artist", term));
            }
            builder.minimumNumberShouldMatch(artistTerms.length);

            boolQueryBuilder.must(builder);
        }

        if (songTerms != null) {
            final BoolQueryBuilder builder = QueryBuilders.boolQuery();
            for (String term : songTerms) {
                builder.should(QueryBuilders.wildcardQuery("torrentSongs.songName", term));
            }
            builder.minimumNumberShouldMatch(songTerms.length > 2 ? songTerms.length - 1 : songTerms.length);

            final NestedQueryBuilder torrentSongs = QueryBuilders.nestedQuery("torrentSongs", builder);

            boolQueryBuilder.must(torrentSongs);
        }

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(page, pageSize));
        searchQueryBuilder.withSort(new FieldSortBuilder("artist").order(SortOrder.ASC));

        Page<TorrentFilesVO> result = torrentFilesRepository.search(searchQueryBuilder.build());

        return result;
    }

    private static String[] asWildcards(String artist) {
        final String lowerCase = artist.toLowerCase();

        final String[] parts = lowerCase.replace("/", " ")
                .replace(".", "?")
                .replace("'", "?")
                .replace("-", "?")
                .replace(",", "?")
                .replace("+", "?")
                .split(";");

        final ArrayList<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                final String trimPart = part.trim();
                terms.add("*" + trimPart + "*");
            }
        }

        return terms.toArray(new String[terms.size()]);
    }


    private boolean isAlreadyDownloaded(File torrentDir) {
        //noinspection ConstantConditions
        return torrentDir.exists() && torrentDir.list().length > 0;
    }

    private boolean isDownloadAllowed() {
        return DOWNLOAD_ALLOWED;
    }


    private Set<TorrentSongVO> getTorrentSongs(String torrentId, File torrentDir, ArtistEntity artistEntity) throws IOException {
        final List<FFileDescriptor> fFileDescriptors = new ArrayList<>();
        final Iterator<File> fileIterator = FileUtils.iterateFiles(torrentDir, new String[]{"cue"}, true);
        while (fileIterator.hasNext()) {
            final File cueFile = fileIterator.next();
            fFileDescriptors.addAll(new CueParser().parseCue(torrentDir, cueFile));
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

    private Priority[] ignoreAllExceptCuePriorities(TorrentInfo torrentInfo, List<String> cueFilePaths) {
        // enlist files
        final int numFiles = torrentInfo.files().numFiles();
        final Priority[] priorities = getPriorities(numFiles);
        for (int j = 0; j < numFiles; j++) {
            final FileStorage fileStorage = torrentInfo.files();
            final String filePath = fileStorage.filePath(j);
            final long fileSize = fileStorage.fileSize(j);
            final String fileName = fileStorage.fileName(j);

//            logger.info("{} ({})", filePath, fileSize);

            if (fileName.endsWith(".cue")) {
                priorities[j] = Priority.NORMAL;
                cueFilePaths.add(filePath);
            }
        }
        return priorities;
    }

    private Priority[] getPriorities(int numFiles) {
        final Priority[] priorities = new Priority[numFiles];
        for (int j = 0; j < numFiles; j++) {
            priorities[j] = Priority.IGNORE;
        }
        return priorities;
    }

    private static String getFolderForTorrent(TorrentInfoVO dumpTorrentInfo) {
        return dumpTorrentInfo.getId();
    }

    /**
     * leave it only to avoid downloading already downloaded files
     */
    @Deprecated
    private static String getLegacyFolderForTorrent(TorrentInfo torrentInfo) {
        return torrentInfo.name();
    }

    private Set<TorrentSongVO> getTorrentSongsMp3(String torrentId, TorrentInfo torrentInfo, ArtistEntity artistEntity) {
        final Set<TorrentSongVO> torrentSongs = new HashSet<>();
        for (int j = 0; j < torrentInfo.files().numFiles(); j++) {
            final FileStorage fileStorage = torrentInfo.files();
            final String filePath = fileStorage.filePath(j);
            final String fileName = fileStorage.fileName(j);

            if (fileName.toLowerCase().endsWith(".mp3")) {
                final String title = fileName.replace(".mp3", "").replace(".MP3", "");
                torrentSongs.add(new TorrentSongVO(torrentId, title, artistEntity.getArtist(), fileName, filePath));
            }
        }
        return torrentSongs;
    }

    private static String[] asTerms(String artist) {
        final String lowerCase = artist.toLowerCase();

        final String[] parts = lowerCase.replace("/", " ")
                .replace("'", " ")
                .replace("&", " ")
                .replace("*", "?")
                .replace(".", "")
                .replace("-", " ")
                .replace("!", " ")
                .replace("_", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replace(",", " ")
                .split(" ");

        final ArrayList<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                final String trimPart = part.trim();
                if (trimPart.equals("и")
                        || trimPart.equals("feat")) {
                    continue;
                }

                terms.add(trimPart);
            }
        }

        return terms.toArray(new String[terms.size()]);
    }

    private static <T extends Comparable> List<T> page(List<? extends T> artistsIds, Integer offset, Integer limit) {
        List<T> result = new ArrayList<>(artistsIds);
        result.sort(Comparable::compareTo);
        if (offset != null && offset < result.size()) {
            result = result.subList(offset, result.size());
        }
        if (limit != null) {
            result = result.subList(0, Math.min(limit, result.size()));
        }
        return result;
    }


    private ResolveResult findResolvedArtistsTorrents(List<Integer> artistsIds) {
        final ResolveResult resolveResult = new ResolveResult();

        {
            final List<String> found = resolveResult.found;
            final List<String> notFound = resolveResult.notFound;

            final Map<String, Future<TorrentInfo>> results = resolveResult.torrents;
            final Map<String, TorrentInfoVO> torrentInfos = resolveResult.torrentInfos;
            final Map<String, ArtistEntity> torrentArtists = resolveResult.torrentArtists;

            // todo asm: add parallelism; save downloaded torrent infos (at least files list)

            for (Integer artistId : artistsIds) {
                final TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(torrentInfoRepository);

                final List<String> torrentIds = new ArrayList<>();
                torrentIds.addAll(playlistSongsMapper.getArtistTorrents2(artistId, MusicFormats.FORMAT_MP3, ResolveStatuses.STATUS_UNKNOWN));
                torrentIds.addAll(playlistSongsMapper.getArtistTorrents2(artistId, MusicFormats.FORMAT_MP3, ResolveStatuses.STATUS_ERROR));

                final String artist = playlistSongsMapper.getArtist(artistId);
                final String[] titleTerms = asTerms(artist);

                logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));

                if (!torrentIds.isEmpty()) {
                    found.add(artist);
                    for (String torrentId : torrentIds) {
                        final TorrentInfoVO torrent = findTorrent(torrentId);
                        if (torrent == null) {
                            throw new AssertionError();
                        }

                        if (!results.containsKey(torrentId)) {
                            torrentInfos.put(torrentId, torrent);

                            final Future<TorrentInfo> torrentInfo = getByMagnetAsync(torrent.getMagnet());
                            results.put(torrentId, torrentInfo);

                            torrentArtists.put(torrentId, new ArtistEntity(artistId, artist));
                        }
                    }
                } else {
                    notFound.add(artist);
                }
            }
        }
        return resolveResult;
    }

    public TorrentSongVO findTorrentSong(String torrentId, String fileId) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        boolQueryBuilder.must(QueryBuilders.termsQuery("torrentId", torrentId));
        boolQueryBuilder.must(QueryBuilders.termsQuery("id", fileId));

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(0, 10));
//        searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC));

        Page<TorrentSongVO> result = torrentSongRepository.search(searchQueryBuilder.build());

        return result.getTotalElements() > 0
                ? result.getContent().iterator().next()
                : null;
    }

    public TorrentInfoVO findTorrent(String torrentId) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        boolQueryBuilder.must(QueryBuilders.termsQuery("id", torrentId));

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(0, 10));
        searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC));

        Page<TorrentInfoVO> result = torrentInfoRepository.search(searchQueryBuilder.build());

        return result.getTotalElements() > 0
                ? result.getContent().iterator().next()
                : null;
    }

    // todo asm: async!!!
    private TorrentInfo getByMagnet(String magnet) {
        return torrentClient.findByMagnet(magnet);
    }
    private CompletableFuture<TorrentInfo> getByMagnetAsync(String magnet) {
        return CompletableFuture.supplyAsync(() -> torrentClient.findByMagnet(magnet), executorService);
    }

    private File downloadCue(TorrentInfo torrentInfo, Priority[] priorities, File targetFolder) {
        if (!targetFolder.exists()) //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
        torrentClient.download(torrentInfo, targetFolder, priorities);
        return targetFolder;
    }


    private Priority[] getSingleFilePriorities(TorrentInfo torrentInfo, String filePath) {
        final FileStorage files = torrentInfo.files();
        final Priority[] priorities = getPriorities(files.numFiles());

        for (int i = 0; i < files.numFiles(); i++) {
            if (files.filePath(i).equals(filePath)) {
                priorities[i] = Priority.NORMAL;
                break;
            }
        }
        return priorities;
    }

    private static void logInfo(String message, Object... args) {
        logger.info(String.format(message, args));
    }

    private static class ResolveResult {

        final List<String> found = new ArrayList<>();
        final List<String> notFound = new ArrayList<>();

        final Map<String, Future<TorrentInfo>> torrents = new HashMap<>();
        final Map<String, TorrentInfoVO> torrentInfos = new HashMap<>();
        final Map<String, ArtistEntity> torrentArtists = new HashMap<>();

    }
}
