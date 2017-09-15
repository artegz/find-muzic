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
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.index.repositories.TorrentSongRepository;
import ru.asm.core.persistence.domain.ArtistEntity;
import ru.asm.core.progress.ProgressBar;
import ru.asm.core.progress.ProgressListener;
import ru.asm.tools.CueParser;
import ru.asm.util.ElasticUtils;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 12:05
 */
@Component
public class TorrentSearcher implements Searcher {

    private static final Logger logger = LoggerFactory.getLogger(TorrentSearcher.class);

    private static final Map<String, String> forumFormats = new HashMap<>();

    public static final String STEP_RESOLVE_MAGNET = "resolving magnet";
    public static final String STEP_DOWNLOAD_FILES = "downloading files";
    public static final String STEP_CONVERT_TO_MP_3 = "converting to mp3";
    public static final String STEP_SAVE_RESULTS = "saving results";
    public static final String STEP_INDEX_FILES = "indexing files";

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
    public void resolveSongSources(Song song, ProgressListener progressListener) {
        final Artist artist = song.getArtist();

        final SongResolveReport songResolveReport = new SongResolveReport(song.getSongId(), artist.getArtistId());
        songResolveReport.setStartTime(new Date());

        // 1. resolve artist torrents stage
        final ArtistDocument artistDocument = resolveArtistTorrents(artist, progressListener, songResolveReport);

        if (songResolveReport.isResolveSucceeded()) {
            // 2. index artist torrents
            final List<TorrentDocument> indexedTorrents = indexArtistTorrents(artistDocument, progressListener, songResolveReport);

            if (songResolveReport.isIndexingSucceeded()) {
                // 3. search song sources (in indexed torrents)
                final List<TorrentSongSource> songSources = searchSongSources(song, indexedTorrents, songResolveReport);
                logger.info("{} sources found", songSources.size());
            } else {
                logger.info("search will be skipped due to indexing wasn't succeeded");
            }
        } else {
            // resolve failed => skip indexing and search
            logger.info("indexing and search will be skipped due to resolve wasn't succeeded");
        }

        songResolveReport.setEndTime(new Date());
        dataStorage.saveSongResolveReport(song.getSongId(), songResolveReport);
    }

    @Override
    public List<TorrentSongSource> getSongSources(Song song) {
        return getSongSourcesFromStorage(song);
    }

    @Override
    public SongResolveReport getLastSongResolveReport(Song song) {
        return dataStorage.getSongResolveReport(song.getSongId());
    }

    @Override
    public void downloadSongs(Song song,
                              List<TorrentSongSource> sources,
                              ProgressListener progressListener) {
        progressListener.initStage(ProgressListener.STAGE_DOWNLOAD,
                Lists.transform(sources, TorrentSongSource::getName),
                String.format("downloading files from %s sources", sources.size())
        );

        final List<TorrentSongSource> mp3Sources = new ArrayList<>();
        final List<TorrentSongSource> flacSources = new ArrayList<>();

        for (TorrentSongSource source : sources) {
            if (source.isMp3()) {
                mp3Sources.add(source);
            } else {
                flacSources.add(source);
            }
        }

        final Map<String, List<FileDocument>> downloadingFiles = new HashMap<>();
        if (!mp3Sources.isEmpty()) {
            downloadingFiles.putAll(downloadMp3Songs(song, mp3Sources, progressListener));
        }
        if (!flacSources.isEmpty()) {
            downloadingFiles.putAll(downloadFlacSongs(song, flacSources, progressListener));
        }

        for (Map.Entry<String, List<FileDocument>> entry : downloadingFiles.entrySet()) {
            final String sourceId = entry.getKey();
            final List<FileDocument> fileDocuments = entry.getValue();

            for (FileDocument fileDocument : fileDocuments) {
                logger.info("[{}] {} downloaded", sourceId, fileDocument.getFsLocation());
            }
        }

        progressListener.completeStage(ProgressListener.STAGE_DOWNLOAD);
    }

    private List<TorrentSongSource> searchSongSources(Song song, List<TorrentDocument> indexedTorrents, SongResolveReport songResolveReport) {
        final String songTitle = song.getTitle();
        final String artistName = song.getArtist().getArtistName();

        logger.info("searching sources for {}: {}", artistName, songTitle);
        final List<TorrentSongSource> songSources = searchSongSourcesImpl(indexedTorrents, songTitle, artistName);
        saveSongSources(song, songSources);

        songResolveReport.setSearchPerformed(true);
        songResolveReport.setFoundSources(Lists.transform(songSources, TorrentSongSource::getSourceId));

        return songSources;
    }

    private List<TorrentDocument> indexArtistTorrents(ArtistDocument artistDocument, ProgressListener progressListener, SongResolveReport songResolveReport) {
        final String artistName = artistDocument.getArtistName();

        progressListener.initStage(ProgressListener.STAGE_INDEX,
                artistDocument.getArtistTorrentIds(),
                String.format("indexing artist %s torrents", artistName)
        );

        final List<TorrentDocument> indexedTorrents = new ArrayList<>();
        logger.info("indexing artist {} torrents...", artistName);
        indexedTorrents.addAll(indexMp3Torrents(artistDocument, progressListener));
        indexedTorrents.addAll(indexFlacTorrents(artistDocument, progressListener));
        logger.info("indexing artist {} torrents complete", artistName);

        songResolveReport.setIndexingPerformed(true);
        for (TorrentDocument indexedTorrent : indexedTorrents) {
            songResolveReport.setTorrentIndexingStatus(indexedTorrent.getTorrentId(), indexedTorrent.getStatus());
        }

        progressListener.completeStage(ProgressListener.STAGE_INDEX);

        return indexedTorrents;
    }

    private ArtistDocument resolveArtistTorrents(Artist artist,
                                                 ProgressListener progressListener,
                                                 SongResolveReport report) {
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

                // start stage
                progressListener.initStage(ProgressListener.STAGE_RESOLVE,
                        Collections.singletonList(artistName),
                        String.format("resolving artist %s torrents", artistName)
                );

                // start search artist torrents step
                progressListener.start(Collections.singletonList(artistName));
                try {
                    // find artists torrents
                    resolveArtistTorrentsImpl(artistId, artistName);
                    logger.info("artist {} torrents resolved", artistName);
                } finally {
                    // complete search artist torrents step
                    progressListener.complete(Collections.singletonList(artistName));
                    // complete stage
                    progressListener.completeStage(ProgressListener.STAGE_RESOLVE);
                }
            } else {
                logger.info("artist {} torrents already resolved", artistName);
            }

            artistDocument = dataStorage.getArtist(artistId);
            assert (!artistDocument.getArtistTorrentIds().isEmpty());

            report.setResolvePerformed(true);
            report.setResolveStatus(SongResolveReport.Status.success);
            report.setResolvedTorrentIds(artistDocument.getArtistTorrentIds());
        } catch (ArtistTorrentsNotFoundException e) {
            report.setResolvePerformed(true);
            report.setResolveStatus(SongResolveReport.Status.failed);
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

    private Map<String, List<FileDocument>> downloadFlacSongs(Song song,
                                                              List<TorrentSongSource> flacSources,
                                                              ProgressListener progressListener) {
        final Map<String, List<FileDocument>> downloadingFiles = new HashMap<>();

        final Map<String, List<TorrentSongSource>> groupedFlacSources = groupPerTorrent(flacSources);
        final Set<String> flacTorrentIds = groupedFlacSources.keySet();

        flacTorrentIds.forEach(torrentId -> {
            final List<TorrentSongSource> torrentSources = groupedFlacSources.get(torrentId);
            final List<TorrentSongSource> requiredSources = new ArrayList<>(torrentSources);
            final List<FileDocument> alreadyExistingFiles = filterAlreadyExisting(requiredSources);

            final List<FileDocument> fileDocuments;

            if (!requiredSources.isEmpty()) {
                final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

                final List<String> startedSteps = Lists.transform(requiredSources, TorrentSongSource::getName);
                progressListener.start(startedSteps);

                final Supplier<List<FileDocument>> filesSupplier = () -> {
                    progressListener.initSubStage(
                            ProgressListener.SUB_STAGE_DOWNLOAD_FLAC_SOURCE,
                            Arrays.asList(STEP_RESOLVE_MAGNET, STEP_DOWNLOAD_FILES, STEP_CONVERT_TO_MP_3, STEP_SAVE_RESULTS),
                            String.format("downloading %s torrent files (flac)", torrentId)
                    );

                    final List<FileDocument> results = new ArrayList<>();

                    final File saveDir = new File(new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE), torrentId);
                    if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                        saveDir.mkdirs();

                    final List<String> requestedCues = getCueFiles(requiredSources);
                    boolean allExist = isAllAlreadyDownloaded(saveDir, requestedCues);
                    if (!allExist) {
                        // 1. resolve magnet
                        progressListener.subStageStart(Collections.singletonList(STEP_RESOLVE_MAGNET));
                        final String magnet = torrent.getTorrentInfo().getMagnet();
                        logger.info("resoling torrent {} info by magnet {}", torrentId, magnet);
                        final TorrentInfo torrentInfo = getByMagnet(magnet);
                        progressListener.subStageComplete(Collections.singletonList(STEP_RESOLVE_MAGNET));

                        logger.info("[{}: {}] magnet resolved, downloading...", song.getArtist().getArtistName(), song.getTitle());

                        // 2. download
                        if (torrentInfo != null) {
                            progressListener.subStageStart(Collections.singletonList(STEP_DOWNLOAD_FILES));
                            final ProgressBar progressBar = progressListener.initSubStageStepProgressBar();
                            final Priority[] priorities = getAllFilesPriorities(torrentInfo, requiredSources);
                            appCoreService.getTorrentClient().download(torrentInfo, saveDir, priorities, progressBar);
                            logger.info("downloading complete: {}", saveDir.getAbsolutePath());
                            progressListener.subStageComplete(Collections.singletonList(STEP_DOWNLOAD_FILES));
                        } else {
                            progressListener.subStageSkip(Collections.singletonList(STEP_DOWNLOAD_FILES));
                        }
                    } else {
                        // skip steps
                        progressListener.subStageSkip(Arrays.asList(STEP_RESOLVE_MAGNET, STEP_DOWNLOAD_FILES));
                    }

                    // 3. flac -> mp3[]
                    progressListener.subStageStart(Collections.singletonList(STEP_CONVERT_TO_MP_3));
                    final FlacMp3Converter flacMp3Converter = new FlacMp3Converter();
                    for (String downloadedCueFile : requestedCues) {
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
                    progressListener.subStageComplete(Collections.singletonList(STEP_CONVERT_TO_MP_3));

                    // 4. update resolved sources
                    progressListener.subStageStart(Collections.singletonList(STEP_SAVE_RESULTS));
                    for (String downloadedCueFile : requestedCues) {

                        // find all sources points to this cue
                        final List<SongSourceDocument> cueBasedSources = this.dataStorage.getSongSourcesByTorrentAndCuePath(torrentId, downloadedCueFile);

                        for (SongSourceDocument cueBasedSource : cueBasedSources) {
                            final String cueFilePath = cueBasedSource.getSongSource().getIndexSong().getCueFilePath();
                            final File folder = getCompositionFilesFolder(saveDir, cueFilePath);

                            final String trackNum = cueBasedSource.getSongSource().getIndexSong().getTrackNum();
                            final String songName = cueBasedSource.getSongSource().getIndexSong().getSongName();

                            final File mp3SongFile = new File(folder, String.format("%s. %s.mp3", trackNum, songName));

                            if (mp3SongFile.exists()) {
                                FileDocument fileDocument = createFileDocument(song, cueBasedSource.getSongSource(), mp3SongFile);
                                dataStorage.insertFile(fileDocument);
                                logger.info("[{}: {}] download complete", song.getArtist().getArtistName(), song.getTitle());
                                results.add(fileDocument);
                            } else {
                                logger.warn("[{}: {}] download failed", song.getArtist().getArtistName(), song.getTitle());
                            }
                        }
                    }
                    progressListener.subStageComplete(Collections.singletonList(STEP_SAVE_RESULTS));

                    progressListener.completeSubStage(
                            ProgressListener.SUB_STAGE_DOWNLOAD_FLAC_SOURCE
                    );

                    return results;
                };

                try {
                    fileDocuments = filesSupplier.get();
                    fileDocuments.addAll(alreadyExistingFiles);
                } finally {
                    progressListener.complete(startedSteps);
                }
            } else {
                fileDocuments = alreadyExistingFiles;
            }

            for (FileDocument fileDocument : fileDocuments) {
                if (!downloadingFiles.containsKey(fileDocument.getSourceId())) {
                    downloadingFiles.put(fileDocument.getSourceId(), new ArrayList<>());
                }
                downloadingFiles.get(fileDocument.getSourceId()).add(fileDocument);
            }
        });
        return downloadingFiles;
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

    private Map<String, List<FileDocument>> downloadMp3Songs(Song song,
                                                             List<TorrentSongSource> mp3Sources,
                                                             ProgressListener progressListener) {
        final Map<String, List<FileDocument>> downloadingFiles = new HashMap<>();

        final Map<String, List<TorrentSongSource>> groupedMp3Sources = groupPerTorrent(mp3Sources);
        final Set<String> mp3TorrentIds = groupedMp3Sources.keySet();

        mp3TorrentIds.forEach(torrentId -> {
            final List<TorrentSongSource> torrentSources = groupedMp3Sources.get(torrentId);
            final List<TorrentSongSource> requiredSources = new ArrayList<>(torrentSources);
            final List<FileDocument> alreadyExistingFiles = filterAlreadyExisting(requiredSources);

            final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

            final List<FileDocument> fileDocumentsResult;
            if (!requiredSources.isEmpty()) {
                final List<String> startedSteps = Lists.transform(requiredSources, TorrentSongSource::getName);
                progressListener.start(startedSteps);

                final Supplier<List<FileDocument>> filesSupplier = () -> {
                    final List<FileDocument> fileDocuments = new ArrayList<>();

                    progressListener.initSubStage(
                            ProgressListener.SUB_STAGE_DOWNLOAD_MP_3_SOURCE,
                            Arrays.asList(STEP_RESOLVE_MAGNET, STEP_DOWNLOAD_FILES, STEP_SAVE_RESULTS),
                            String.format("downloading %s torrent files (mp3)", torrentId)
                    );

                    progressListener.subStageStart(Collections.singletonList(STEP_RESOLVE_MAGNET));
                    final String magnet = torrent.getTorrentInfo().getMagnet();
                    logger.info("resoling torrent {} info by magnet {}", torrentId, magnet);
                    TorrentInfo torrentInfo = getByMagnet(magnet);
                    progressListener.subStageComplete(Collections.singletonList(STEP_RESOLVE_MAGNET));

                    final Set<String> requiredFiles = getRequiredMp3FilePaths(requiredSources);

                    if (!requiredFiles.isEmpty()) {

                        progressListener.subStageStart(Collections.singletonList(STEP_DOWNLOAD_FILES));
                        final ProgressBar progressBar = progressListener.initSubStageStepProgressBar();
                        logger.info("[{}: {}] magnet resolved, downloading '{}'", song.getArtist().getArtistName(), song.getTitle(), requiredFiles);
                        final Priority[] priorities = getRequiredFilesPriorities(torrentInfo, requiredFiles);
                        final File saveDir = new File(new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE), torrentId);
                        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                            saveDir.mkdirs();
                        appCoreService.getTorrentClient().download(torrentInfo, saveDir, priorities, progressBar);
                        progressListener.subStageComplete(Collections.singletonList(STEP_DOWNLOAD_FILES));

                        progressListener.subStageStart(Collections.singletonList(STEP_SAVE_RESULTS));
                        for (TorrentSongSource requiredSource : requiredSources) {
                            final String mp3FilePath = requiredSource.getIndexSong().getMp3FilePath();
                            if (mp3FilePath == null) {
                                continue;
                            }

                            final File downloadedSong = new File(saveDir, mp3FilePath);
                            if (downloadedSong.exists()) {
                                FileDocument fileDocument = createFileDocument(song, requiredSource, downloadedSong);

                                dataStorage.insertFile(fileDocument);
                                logger.info("[{}: {}] download complete", song.getArtist().getArtistName(), song.getTitle(), mp3FilePath);

                                fileDocuments.add(fileDocument);
                            } else {
                                logger.warn("[{}: {}] download failed", song.getArtist().getArtistName(), song.getTitle(), mp3FilePath);
                            }
                        }
                        progressListener.subStageComplete(Collections.singletonList(STEP_SAVE_RESULTS));

                    } else {
                        logger.warn("nothing to download (torrent {})", torrentId);
                        progressListener.subStageSkip(Arrays.asList(STEP_DOWNLOAD_FILES, STEP_SAVE_RESULTS));
                    }

                    progressListener.completeSubStage(
                            ProgressListener.SUB_STAGE_DOWNLOAD_MP_3_SOURCE
                    );

                    return fileDocuments;
                };
                try {
                    fileDocumentsResult = filesSupplier.get();
                    fileDocumentsResult.addAll(alreadyExistingFiles);
                } finally {
                    progressListener.complete(startedSteps);
                }
            } else {
                fileDocumentsResult = alreadyExistingFiles;
            }

            for (FileDocument fileDocument : fileDocumentsResult) {
                if (!downloadingFiles.containsKey(fileDocument.getSourceId())) {
                    downloadingFiles.put(fileDocument.getSourceId(), new ArrayList<>());
                }
                downloadingFiles.get(fileDocument.getSourceId()).add(fileDocument);
            }
        });
        return downloadingFiles;
    }

    private FileDocument createFileDocument(Song song, TorrentSongSource requiredSource, File downloadedSong) {
        FileDocument fileDocument = new FileDocument();
        fileDocument.setId(System.nanoTime());
        fileDocument.setFsLocation(downloadedSong.getAbsolutePath());
        fileDocument.setSongId(song.getSongId());
        fileDocument.setSourceId(requiredSource.getSourceId());
        return fileDocument;
    }

    private Set<String> getRequiredMp3FilePaths(List<TorrentSongSource> requiredSources) {
        final Set<String> requiredFiles = new HashSet<>();
        for (TorrentSongSource requiredSource : requiredSources) {
            if (requiredSource.getIndexSong().getMp3FilePath() != null) {
                requiredFiles.add(requiredSource.getIndexSong().getMp3FilePath());
            }
        }
        return requiredFiles;
    }

    private List<FileDocument> filterAlreadyExisting(List<TorrentSongSource> requiredSources) {
        final List<FileDocument> existingFiles = new ArrayList<>();

        final Iterator<TorrentSongSource> it = requiredSources.iterator();
        while (it.hasNext()) {
            final TorrentSongSource source = it.next();
            final FileDocument existingFile = dataStorage.getFileBySource(source.getSourceId());
            if (existingFile != null) {
                it.remove();
                existingFiles.add(existingFile);
            }
        }
        return existingFiles;
    }

    private Map<String, List<TorrentSongSource>> groupPerTorrent(List<TorrentSongSource> sources) {
        final Map<String, List<TorrentSongSource>> grouped = new HashMap<>();
        for (TorrentSongSource source : sources) {
            final String torrentId = source.getIndexSong().getTorrentId();
            if (!grouped.containsKey(torrentId)) {
                grouped.put(torrentId, new ArrayList<>());
            }
            grouped.get(torrentId).add(source);
        }
        return grouped;
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

    private List<String> getCueFiles(List<TorrentSongSource> songSources) {
        final Set<String> cueFilesPaths = new HashSet<>(Lists.transform(songSources, input -> {
            assert (input != null);
            return input.getIndexSong().getCueFilePath();
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

    private boolean isArtistTorrentsResolved(Integer artistId) {
        final ArtistDocument artist = dataStorage.getArtist(artistId);
        return artist != null && artist.getArtistTorrentIds() != null && !artist.getArtistTorrentIds().isEmpty();
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

            final Page<TorrentInfoVO> page = findTorrentsByTitle(new String[] {forumId}, titleTerms);

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


    private List<TorrentDocument> indexFlacTorrents(ArtistDocument artistDocument, ProgressListener progressListener) {
        logger.info("indexing FLAC torrents...");

        final List<TorrentDocument> indexedTorrents = new ArrayList<>();

        // temp directory for FLAC mappings
        File saveDir = new File("C:\\TEMP\\find-music\\downloads\\flac\\");
        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
            saveDir.mkdirs();

        final ArtistEntity artistEntity = new ArtistEntity(artistDocument.getArtistId(), artistDocument.getArtistName());
        logger.info("{} torrents found", artistDocument.getArtistTorrentIds().size(), artistDocument.getArtistName());

        for (String torrentId : artistDocument.getArtistTorrentIds()) {
            progressListener.start(Collections.singletonList(torrentId));

            logger.info("indexing {} torrent...", torrentId);

            final TorrentDocument torrentDocument = dataStorage.getTorrent(torrentId);
            final TorrentInfoVO dumpTorrentInfo = torrentDocument.getTorrentInfo();

            if (!torrentDocument.getFormat().equals(MusicFormats.FORMAT_FLAC)) {
                logger.info("skipping torrent {} due to it's not FLAC torrent", torrentId);
                continue;
            }

            if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_UNKNOWN)
                    || torrentDocument.getStatus().equals(ResolveStatuses.STATUS_ERROR)) {
                final Supplier<TorrentDocument> torrentDocumentSupplier = () -> {

                    progressListener.initSubStage(
                            ProgressListener.SUB_STAGE_INDEX_FLAC_SOURCE,
                            Arrays.asList(STEP_RESOLVE_MAGNET, STEP_DOWNLOAD_FILES, STEP_INDEX_FILES),
                            String.format("indexing %s torrent (flac)", torrentId)
                    );

                    Boolean error = false;
                    final File torrentDir = new File(saveDir, getFolderForTorrent(dumpTorrentInfo));

                    final Set<TorrentSongVO> torrentSongs = new HashSet<>();

                    //noinspection ConstantConditions
                    if (!isAlreadyDownloaded(torrentDir)) {

                        // find torrent info by magnet
                        progressListener.subStageStart(Collections.singletonList(STEP_RESOLVE_MAGNET));
                        logger.info("fetching torrent info by magnet...");
                        final TorrentInfo torrentInfo = getByMagnet(dumpTorrentInfo.getMagnet());
                        logger.info("fetch completed");
                        progressListener.subStageComplete(Collections.singletonList(STEP_RESOLVE_MAGNET));
                        if (torrentInfo != null) {
                            progressListener.subStageStart(Collections.singletonList(STEP_DOWNLOAD_FILES));

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
                                final ProgressBar subStageProgressBar = progressListener.initSubStageStepProgressBar();
                                logger.info("downloading cue file...");
                                downloadCue(torrentInfo, priorities, torrentDir, subStageProgressBar);
                                logger.info("download complete");
                            } else {
                                error = true;
                            }

                            progressListener.subStageComplete(Collections.singletonList(STEP_DOWNLOAD_FILES));
                        } else {
                            logger.debug("fetch torrent info has failed");
                            error = true;
                            progressListener.subStageSkip(Collections.singletonList(STEP_DOWNLOAD_FILES));
                        }
                    } else {
                        logger.info("torrent {} is already downloaded", torrentId);
                        progressListener.subStageSkip(Arrays.asList(STEP_RESOLVE_MAGNET, STEP_DOWNLOAD_FILES));
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

                    progressListener.completeSubStage(
                            ProgressListener.SUB_STAGE_INDEX_FLAC_SOURCE
                    );

                    return torrentDocument;
                };
//                if (async) {
//                    indexedTorrents.add(
//                            CompletableFuture.supplyAsync(torrentDocumentSupplier, executor)
//                                    .whenComplete((td, throwable) -> progressListener.complete(Collections.singletonList(torrentId)))
//                    );
//                } else {
//                    indexedTorrents.add(CompletableFuture.completedFuture(torrentDocumentSupplier.get()));
//                    progressListener.complete(Collections.singletonList(torrentId));
//                }
                indexedTorrents.add(torrentDocumentSupplier.get());
                progressListener.complete(Collections.singletonList(torrentId));
            } else {
                logger.info("torrent {} is already indexed", torrentId);
//                indexedTorrents.add(CompletableFuture.completedFuture(torrentDocument));
                indexedTorrents.add(torrentDocument);
                progressListener.complete(Collections.singletonList(torrentId));
            }
        }

        return indexedTorrents;
    }


    private List<TorrentDocument> indexMp3Torrents(ArtistDocument artistDocument, ProgressListener progressListener) {
        logger.info("indexing MP3 torrents...");
        logger.info("{} torrents found", artistDocument.getArtistTorrentIds().size(), artistDocument.getArtistName());

        final List<TorrentDocument> indexedTorrents = new ArrayList<>();

        for (String torrentId : artistDocument.getArtistTorrentIds()) {
            progressListener.start(Collections.singletonList(torrentId));

            final TorrentDocument torrentDocument = dataStorage.getTorrent(torrentId);

            if (!torrentDocument.getFormat().equals(MusicFormats.FORMAT_MP3)) {
                logger.debug("skipping torrent {} due to it's not MP3 torrent", torrentId);
                continue;
            }

            if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_UNKNOWN)
                    || torrentDocument.getStatus().equals(ResolveStatuses.STATUS_ERROR)) {
                logger.info("indexing torrent {} (previous status {})", torrentId, torrentDocument.getStatus());

                final Supplier<TorrentDocument> torrentDocumentSupplier = () -> {
                    progressListener.initSubStage(
                            ProgressListener.SUB_STAGE_INDEX_MP_3_SOURCE,
                            Collections.singletonList(STEP_RESOLVE_MAGNET),
                            String.format("indexing %s torrent (mp3)", torrentId)
                    );

                    progressListener.subStageStart(Collections.singletonList(STEP_RESOLVE_MAGNET));
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
                        final Set<TorrentSongVO> torrentSongs = getTorrentSongsMp3(torrentId, torrentInfo, artistDocument.getArtistName());
                        torrentDocument.setStatus(ResolveStatuses.STATUS_OK);

                        // index torrent songs
                        if (!torrentSongs.isEmpty()) {
                            logger.info("{} songs indexed in torrent {}", torrentSongs.size(), torrentId);
                            torrentSongRepository.save(torrentSongs);
                        } else {
                            logger.info("no songs indexed in torrent {}", torrentId);
                        }
                    }
                    progressListener.subStageComplete(Collections.singletonList(STEP_RESOLVE_MAGNET));

                    // update torrent information
                    dataStorage.updateTorrent(torrentDocument);

                    progressListener.completeSubStage(
                            ProgressListener.SUB_STAGE_INDEX_MP_3_SOURCE
                    );

                    return torrentDocument;
                };
//                if (async) {
//                    indexedTorrents.add(
//                            CompletableFuture.supplyAsync(torrentDocumentSupplier, executor)
//                                    .whenComplete((td, throwable) -> progressListener.complete(Collections.singletonList(torrentId)))
//                    );
//                } else {
//                    indexedTorrents.add(
//                            CompletableFuture.completedFuture(torrentDocumentSupplier.get())
//                    );
//                    progressListener.complete(Collections.singletonList(torrentId));
//                }
                indexedTorrents.add(torrentDocumentSupplier.get());
                progressListener.complete(Collections.singletonList(torrentId));
            } else {
                logger.info("torrent {} is already indexed", torrentId);
//                indexedTorrents.add(CompletableFuture.completedFuture(torrentDocument));
                indexedTorrents.add(torrentDocument);
                progressListener.complete(Collections.singletonList(torrentId));
            }
        }

        return indexedTorrents;
    }

    private TorrentDocument awaitFutureNoError(TorrentDocument indexedTorrent) {
//        final TorrentDocument torrentDocument;
//        try {
//            torrentDocument = indexedTorrent.get();
//        } catch (InterruptedException | ExecutionException e) {
//            logger.error(e.getMessage());
//            throw new RuntimeException(e);
//        }
//        return torrentDocument;
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

    private File downloadCue(TorrentInfo torrentInfo, Priority[] priorities, File targetFolder, ProgressBar progressBar) {
        if (!targetFolder.exists()) //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
        appCoreService.getTorrentClient().download(torrentInfo, targetFolder, priorities, progressBar);
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

}
