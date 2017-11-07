package ru.asm.core.dev.model.torrent;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;
import ru.asm.core.AppConfiguration;
import ru.asm.core.AppCoreService;
import ru.asm.core.dev.model.*;
import ru.asm.core.dev.model.ddb.*;
import ru.asm.core.flac.FFileDescriptor;
import ru.asm.core.flac.FTrackDescriptor;
import ru.asm.core.flac.FlacMp3Converter;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.index.repositories.TorrentSongRepository;
import ru.asm.core.persistence.domain.ArtistEntity;
import ru.asm.core.progress.TaskProgress;
import ru.asm.core.flac.CueParser;
import ru.asm.util.ElasticUtils;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 12:05
 */
@Component
public class TorrentSearcher implements Searcher {

    private static final Logger logger = LoggerFactory.getLogger(TorrentSearcher.class);

    private static final Map<String, String> forumFormats = new HashMap<>();

    static {
        forumFormats.put("737", MusicFormats.FORMAT_FLAC); // Рок, Панк, Альтернатива (lossless)
        forumFormats.put("738", MusicFormats.FORMAT_MP3); // Рок, Панк, Альтернатива (lossy)
    }


    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private TorrentSongRepository torrentSongRepository;

    @Autowired
    private TorrentInfoRepository torrentInfoRepository;

    @Autowired
    private AppCoreService appCoreService;

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
    public List<TorrentSongSource> getSongSources(Song song) {
        return getSongSourcesFromStorage(song);
    }

    @Override
    public ArtistResolveReport getLastSongResolveReport(Artist artist) {
        return dataStorage.getSongResolveReport(artist.getArtistId());
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
                downloadedSongs = new Mp3Downloader().downloadTorrent(torrentId, artist, requestedSongs, taskProgress);
            } else {
                downloadedSongs = new FlacDownloader().downloadTorrent(torrentId, artist, requestedSongs, taskProgress);
            }
            for (SongFileDocument downloadedSong : downloadedSongs) {
                logger.info("[{}] {} downloaded", downloadedSong.songSource.getSourceId(), downloadedSong.getFileDocument().getFsLocation());
            }
        } finally {
            taskProgress.completeSubTask(tId);
        }
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

    private File getCompositionFilesFolder(File saveDir, String cueFilePath) {
        final String folderName = getFolder(cueFilePath);
        final String compositionName = getCompositionName(cueFilePath);

        File folder = new File(saveDir, folderName);
        folder = new File(folder, compositionName);
        return folder;
    }

    private boolean isAllAlreadyDownloaded(File saveDir, List<String> requestedCues) {
        boolean allExist = true;
        for (String requestedCue : requestedCues) {
            final String folder = getFolder(requestedCue);
            final String compositionName = getCompositionName(requestedCue);

            final File directory = new File(saveDir, folder);
            final File cue = new File(directory, compositionName + ".cue");
            final File flac = new File(directory, compositionName + ".flac");

            if (!cue.exists() || !flac.exists()) {
                allExist = false;
                break;
            }
        }
        return allExist;
    }

    private String getCompositionName(String downloadedCueFile) {
        return downloadedCueFile.substring(downloadedCueFile.lastIndexOf("\\") + 1, downloadedCueFile.lastIndexOf("."));
    }

    private String getFolder(String cueFilePath) {
        return cueFilePath.substring(0, cueFilePath.lastIndexOf("\\"));
    }

    private FileDocument createFileDocument(Song song, TorrentSongSource requiredSource, File downloadedSong) {
        FileDocument fileDocument = new FileDocument();
        fileDocument.setId(System.nanoTime());
        fileDocument.setFsLocation(downloadedSong.getAbsolutePath());
        fileDocument.setSongId(song.getSongId());
        fileDocument.setSourceId(requiredSource.getSourceId());
        return fileDocument;
    }

    private Set<String> getRequiredMp3FilePaths2(List<SongFileDocument> requiredSources) {
        final Set<String> requiredFiles = new HashSet<>();
        for (SongFileDocument requiredSource : requiredSources) {
            if (requiredSource.getSongSource().getIndexSong().getMp3FilePath() != null) {
                requiredFiles.add(requiredSource.getSongSource().getIndexSong().getMp3FilePath());
            }
        }
        return requiredFiles;
    }

    private List<SongFileDocument> filterAlreadyExisting2(List<SongFileDocument> requiredSources) {
        final List<SongFileDocument> existingFiles = new ArrayList<>();

        final Iterator<SongFileDocument> it = requiredSources.iterator();
        while (it.hasNext()) {
            final SongFileDocument source = it.next();
            final FileDocument existingFile = dataStorage.getFileBySource(source.getSongSource().getSourceId());
            if (existingFile != null) {
                it.remove();
                existingFiles.add(new SongFileDocument(source.song, source.songSource, existingFile));
            }
        }
        return existingFiles;
    }

    @Override
    public List<FileDocument> getDownloadedSongs(Song song) {
        final List<FileDocument> files = dataStorage.getFiles(song.getSongId());
        final TreeSet<FileDocument> uniqueSet = new TreeSet<>(Comparator.comparing(FileDocument::getFsLocation));
        uniqueSet.addAll(files);
        return new ArrayList<>(uniqueSet);
    }

    private Priority[] getPriorities(int numFiles) {
        final Priority[] priorities = new Priority[numFiles];
        for (int j = 0; j < numFiles; j++) {
            priorities[j] = Priority.IGNORE;
        }
        return priorities;
    }

    private Priority[] getRequiredFilesPriorities(TorrentInfo torrentInfo, Set<String> requiredFiles) {
        final FileStorage files = torrentInfo.files();
        final Priority[] priorities = getPriorities(files.numFiles());

        for (int i = 0; i < files.numFiles(); i++) {
            if (requiredFiles.contains(files.filePath(i))) {
                priorities[i] = Priority.NORMAL;
            }
        }
        return priorities;
    }

    private List<String> getCueFiles2(List<SongFileDocument> songSources) {
        final Set<String> cueFilesPaths = new HashSet<>(Lists.transform(songSources, input -> {
            assert (input != null);
            return input.getSongSource().getIndexSong().getCueFilePath();
        }));
        return new ArrayList<>(cueFilesPaths);
    }


    private Priority[] getAllFilesPriorities(TorrentInfo torrentInfo, List<TorrentSongSource> songSources) {
        final FileStorage files = torrentInfo.files();
        final Priority[] priorities = getPriorities(files.numFiles());

        final List<String> filesToDownload = new ArrayList<>();

        for (int i = 0; i < files.numFiles(); i++) {
            final String filePath = files.filePath(i);
            final String fileName = files.fileName(i);
            if (isDownloadFile(filePath, fileName, songSources)) {
                priorities[i] = Priority.NORMAL;
                filesToDownload.add(filePath);
            } else {
                priorities[i] = Priority.IGNORE;
            }
        }

        logger.info("downloading files:");
        for (String file : filesToDownload) {
            logger.info(file);
        }

        return priorities;
    }

    private boolean isDownloadFile(String filePath, String fileName, List<TorrentSongSource> songSources) {
        // todo asm: check
        boolean download = false;
        for (TorrentSongSource songSource : songSources) {
            final String cueFilePath = songSource.getIndexSong().getCueFilePath();
            final int cueFileExtensionPosition = cueFilePath.lastIndexOf(".");
            if (cueFileExtensionPosition < 0) {
                logger.error("bad file {}", cueFilePath);
                continue;
            }
            final int cueFilePathBeginPosition = cueFilePath.startsWith("\\")
                    ? 1
                    : 0;
            final String cueFilePathWithoutExtension = cueFilePath.substring(cueFilePathBeginPosition, cueFileExtensionPosition);

            final int fileExtensionPosition = filePath.lastIndexOf(".");
            if (fileExtensionPosition < 0) {
                // file without extension, skip
                continue;
            }
            final String filePathWithoutExtension = filePath.substring(0, fileExtensionPosition);

            if (filePathWithoutExtension.equals(cueFilePathWithoutExtension)) {
                download = true;
            }
        }

        return download;
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
        appCoreService.getTorrentClient().download(torrentInfo, targetFolder, priorities, taskProgress);
        return targetFolder;
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

    private boolean isDownloadAllowed() {
        return AppConfiguration.DOWNLOAD_ALLOWED;
    }

    /**
     * leave it only to avoid downloading already downloaded files
     */
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

    private List<TorrentSongSource> getSongSourcesFromStorage(Song song) {
        final List<SongSourceDocument> songSources = dataStorage.getSongSources(song.getSongId());
        return Lists.transform(songSources, SongSourceDocument::getSongSource);
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

    private static class SongFileDocument {

        private Song song;

        private TorrentSongSource songSource;

        private FileDocument fileDocument;

        SongFileDocument(Song song, TorrentSongSource songSource, FileDocument fileDocument) {
            this.song = song;
            this.songSource = songSource;
            this.fileDocument = fileDocument;
        }

        public Song getSong() {
            return song;
        }

        public FileDocument getFileDocument() {
            return fileDocument;
        }

        public TorrentSongSource getSongSource() {
            return songSource;
        }
    }

    private class FlacDownloader {

        private List<SongFileDocument> downloadTorrent(String torrentId,
                                                       Artist artist,
                                                       List<SongFileDocument> requestedSongs,
                                                       TaskProgress taskProgress) {

            final List<SongFileDocument> result = new ArrayList<>();

            final List<SongFileDocument> notDownloadedRequestedSongs = new ArrayList<>(requestedSongs);
            final List<SongFileDocument> alreadyDownloadedRequestedSongs = filterAlreadyExisting2(notDownloadedRequestedSongs);

            final List<SongFileDocument> downloadedRequestedSongs;
            if (!notDownloadedRequestedSongs.isEmpty()) {
                final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

                final List<SongFileDocument> results = new ArrayList<>();

                final File saveDir = new File(new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE), torrentId);
                if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                    saveDir.mkdirs();

                boolean allExist = isAllAlreadyDownloaded(saveDir, getCueFiles2(notDownloadedRequestedSongs));
                if (!allExist) {
                    // 1. resolve magnet
                    taskProgress.log("resolving magnet...");
                    final String magnet = torrent.getTorrentInfo().getMagnet();
                    logger.info("resoling torrent {} info by magnet {}", torrentId, magnet);
                    final TorrentInfo torrentInfo = getByMagnet(magnet);

                    logger.info("[{}] magnet resolved, downloading...", artist.getArtistName());

                    // 2. download
                    if (torrentInfo != null) {
                        taskProgress.log("downloading files...");
                        final Priority[] priorities = getAllFilesPriorities(torrentInfo, Lists.transform(notDownloadedRequestedSongs, SongFileDocument::getSongSource));
                        appCoreService.getTorrentClient().download(torrentInfo, saveDir, priorities, taskProgress);
                        logger.info("downloading complete: {}", saveDir.getAbsolutePath());
                    }
                }

                // 3. flac -> mp3[]
                taskProgress.log("converting FLAC to MP3...");
                final FlacMp3Converter flacMp3Converter = new FlacMp3Converter();
                for (SongFileDocument notDownloadedRequestedSong : notDownloadedRequestedSongs) {
                    final String downloadedCueFile = notDownloadedRequestedSong.getSongSource().getIndexSong().getCueFilePath();

                    final File compositionFilesFolder = getCompositionFilesFolder(saveDir, downloadedCueFile);
                    //noinspection ConstantConditions
                    if (compositionFilesFolder.exists()
                            && compositionFilesFolder.listFiles() != null
                            && compositionFilesFolder.listFiles().length > 0) {
                        continue; // already split
                    }

                    final String folder = getFolder(downloadedCueFile);
                    final String compositionName = getCompositionName(downloadedCueFile);

                    final File directory = new File(saveDir, folder);
                    flacMp3Converter.convert(directory, compositionName);
                }

                // 4. update resolved sources
                taskProgress.log("saving results...");
                for (SongFileDocument notDownloadedRequestedSong : notDownloadedRequestedSongs) {
                    final String downloadedCueFile = notDownloadedRequestedSong.getSongSource().getIndexSong().getCueFilePath();
                    final Song song = notDownloadedRequestedSong.getSong();

                    // find all sources points to this cue
                    final List<SongSourceDocument> cueBasedSources = dataStorage.getSongSourcesByTorrentAndCuePath(torrentId, downloadedCueFile);

                    for (SongSourceDocument cueBasedSource : cueBasedSources) {
                        final String cueFilePath = cueBasedSource.getSongSource().getIndexSong().getCueFilePath();
                        final File folder = getCompositionFilesFolder(saveDir, cueFilePath);

                        final String trackNum = cueBasedSource.getSongSource().getIndexSong().getTrackNum();
                        final String songName = cueBasedSource.getSongSource().getIndexSong().getSongName();

                        final File mp3SongFile = new File(folder, String.format("%s. %s.mp3", trackNum, songName));

                        if (mp3SongFile.exists()) {
                            FileDocument fileDocument = createFileDocument(song, cueBasedSource.getSongSource(), mp3SongFile);
                            dataStorage.insertFile(fileDocument);
                            logger.info("[{}: {}] download complete", artist.getArtistName(), song.getTitle());
                            results.add(new SongFileDocument(song, cueBasedSource.getSongSource(), fileDocument));
                        } else {
                            logger.warn("[{}: {}] download failed", artist.getArtistName(), song.getTitle());
                        }
                    }
                }

                downloadedRequestedSongs = results;
                downloadedRequestedSongs.addAll(alreadyDownloadedRequestedSongs);

            } else {
                downloadedRequestedSongs = alreadyDownloadedRequestedSongs;
            }

            result.addAll(downloadedRequestedSongs);

            return result;
        }

    }

    private class Mp3Downloader {

        private List<SongFileDocument> downloadTorrent(String torrentId,
                                                       Artist artist,
                                                       List<SongFileDocument> requestedSongs,
                                                       TaskProgress taskProgress) {
            final List<SongFileDocument> resultSongsWithFile = new ArrayList<>();

            final List<SongFileDocument> notDownloadedRequestedSongs = new ArrayList<>(requestedSongs);
            final List<SongFileDocument> alreadyDownloadedRequestedSongsWithFile = filterAlreadyExisting2(notDownloadedRequestedSongs);

            final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

            final List<SongFileDocument> downloadedRequestedSongsWithFile;
            if (!notDownloadedRequestedSongs.isEmpty()) {
                final List<SongFileDocument> downloadedSongsWithFile = new ArrayList<>();

                taskProgress.log("resolving magnet...");
                final String magnet = torrent.getTorrentInfo().getMagnet();
                logger.info("resoling torrent {} info by magnet {}", torrentId, magnet);
                TorrentInfo torrentInfo = getByMagnet(magnet);

                final Set<String> requiredFilesPaths = getRequiredMp3FilePaths2(notDownloadedRequestedSongs);

                if (!requiredFilesPaths.isEmpty()) {

                    taskProgress.log("downloading files...");
                    logger.info("[{}] magnet resolved, downloading '{}'", artist.getArtistName(), requiredFilesPaths);
                    final Priority[] priorities = getRequiredFilesPriorities(torrentInfo, requiredFilesPaths);
                    final File saveDir = new File(new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE), torrentId);
                    if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                        saveDir.mkdirs();
                    appCoreService.getTorrentClient().download(torrentInfo, saveDir, priorities, taskProgress);

                    taskProgress.log("saving downloaded files...");
                    for (SongFileDocument notDownloadedRequestedSong : notDownloadedRequestedSongs) {
                        final Song song = notDownloadedRequestedSong.getSong();

                        final String mp3FilePath = notDownloadedRequestedSong.getSongSource().getIndexSong().getMp3FilePath();
                        if (mp3FilePath == null) {
                            continue;
                        }

                        final File downloadedSongFile = new File(saveDir, mp3FilePath);
                        if (downloadedSongFile.exists()) {
                            final FileDocument fileDocument = createFileDocument(song, notDownloadedRequestedSong.getSongSource(), downloadedSongFile);

                            dataStorage.insertFile(fileDocument);
                            logger.info("[{}: {}] download complete", artist.getArtistName(), song.getTitle(), mp3FilePath);

                            downloadedSongsWithFile.add(new SongFileDocument(song, notDownloadedRequestedSong.getSongSource(), fileDocument));
                        } else {
                            logger.warn("[{}: {}] download failed", artist.getArtistName(), song.getTitle(), mp3FilePath);
                        }
                    }

                } else {
                    logger.warn("nothing to download (torrent {})", torrentId);
                }

                downloadedRequestedSongsWithFile = downloadedSongsWithFile;
                downloadedRequestedSongsWithFile.addAll(alreadyDownloadedRequestedSongsWithFile);
            } else {
                downloadedRequestedSongsWithFile = alreadyDownloadedRequestedSongsWithFile;
            }

            resultSongsWithFile.addAll(downloadedRequestedSongsWithFile);

            return resultSongsWithFile;
        }
    }
}
