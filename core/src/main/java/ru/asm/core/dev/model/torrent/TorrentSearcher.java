package ru.asm.core.dev.model.torrent;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.TorrentInfo;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;
import ru.asm.core.AppCoreService;
import ru.asm.core.dev.model.Searcher;
import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.SongResult;
import ru.asm.core.dev.model.SongSource;
import ru.asm.core.dev.model.ddb.ArtistDocument;
import ru.asm.core.dev.model.ddb.DataStorage;
import ru.asm.core.dev.model.ddb.TorrentDocument;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentSongRepository;
import ru.asm.util.ElasticUtils;
import ru.asm.util.ResolveStatuses;

import java.util.*;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 12:05
 */
@Component
public class TorrentSearcher implements Searcher {

    private static final Logger logger = LoggerFactory.getLogger(TorrentSearcher.class);

    @Autowired
    private IndexedArtistsService indexedArtistsService;

    @Override
    public List<SongSource> search(Song song) {
        final Integer artistId = song.getArtist().getArtistId();
        if (!indexedArtistsService.isArtistIndexed(artistId)) {
            // find artists torrents
            // index torrents songs
            indexedArtistsService.indexArtist(song.getArtist());
        }

        final List<SongSource> songSources = findMp3SongSources(song);

        // find all matching song sources
        return songSources;
    }



    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private TorrentSongRepository torrentSongRepository;

    @Autowired
    private AppCoreService appCoreService;

    public List<SongSource> findMp3SongSources(Song song) {
        final List<Mp3TorrentSongSource> songSources = new ArrayList<>();

        final ArtistDocument artistDocument = dataStorage.getArtist(song.getArtist().getArtistId());

        for (String torrentId : artistDocument.getArtistTorrentIds()) {
            final TorrentDocument torrentDocument = dataStorage.getTorrent(torrentId);

            if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_UNKNOWN)
                    || torrentDocument.getStatus().equals(ResolveStatuses.STATUS_ERROR)) {
                final TorrentInfo torrentInfo = getByMagnet(torrentDocument.getTorrentInfo().getMagnet());

                if (torrentInfo == null) {
                    // not found => error
                    torrentDocument.setStatus(ResolveStatuses.STATUS_ERROR);
                } else {
                    // found
                    final Set<TorrentSongVO> torrentSongs = getTorrentSongsMp3(torrentId, torrentInfo, artistDocument.getArtistName());
                    torrentDocument.setTorrentSongs(new ArrayList<>(torrentSongs));
                    torrentDocument.setStatus(ResolveStatuses.STATUS_OK);

                    torrentSongRepository.save(torrentSongs);
                }

                dataStorage.updateTorrent(torrentDocument);
            }

            final String[] songTerms = ElasticUtils.asTerms3(song.getTitle());
            logger.info(String.format("SEARCH %s: %s (%s)", song.getArtist(), song.getTitle(), Arrays.asList(songTerms)));

            final Page<TorrentSongVO> indexSongs = findSongs(torrentId, songTerms);
            if (indexSongs.hasContent()) {
                for (TorrentSongVO indexSong : indexSongs) {
                    final Mp3TorrentSongSource songSource = new Mp3TorrentSongSource(indexSong);
                    songSources.add(songSource);
                }
            }
        }

        return new ArrayList<>(songSources);
    }

    private Page<TorrentSongVO> findSongs(String torrentId, String[] songTerms) {
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

    private TorrentInfo getByMagnet(String magnet) {
        return appCoreService.getTorrentClient().findByMagnet(magnet);
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



    /*

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

    */



















    public SongResult fetch(Song song, List<SongSource> sources) {
        return null;
    }
}
