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
import ru.asm.core.dev.model.Artist;
import ru.asm.core.dev.model.FlacMp3Converter;
import ru.asm.core.dev.model.Searcher;
import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.ddb.*;
import ru.asm.core.flac.FFileDescriptor;
import ru.asm.core.flac.FTrackDescriptor;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.index.repositories.TorrentSongRepository;
import ru.asm.core.persistence.domain.ArtistEntity;
import ru.asm.tools.CueParser;
import ru.asm.util.ElasticUtils;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

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


    private ExecutorService executor = Executors.newFixedThreadPool(8);


    @Override
    public void resolveSongSources(Song song) {
        final Integer artistId = song.getArtist().getArtistId();
        if (!isArtistTorrentsResolved(artistId)) {
            logger.info("resolving artist {} ({}) torrents", song.getArtist().getArtistName(), song.getArtist().getArtistId());
            // find artists torrents
            resolveArtistTorrents(song.getArtist());
            logger.info("artist {} torrents resolved", song.getArtist().getArtistName());
        } else {
            logger.info("artist {} torrents already resolved", song.getArtist().getArtistName());
        }

        final List<Future<TorrentDocument>> indexedTorrents = new ArrayList<>();
        logger.info("searching sources for {}: {}", song.getArtist().getArtistName(), song.getTitle());
        indexedTorrents.addAll(indexMp3Torrents(song));
        indexedTorrents.addAll(indexFlacTorrents(song));

        final List<TorrentSongSource> songSources = searchSongSources(song, indexedTorrents);

        logger.info("{} sources found", songSources.size());

        saveSongSources(song, songSources);
    }

    @Override
    public List<TorrentSongSource> getSongSources(Song song) {
        return getSongSourcesFromStorage(song);
    }

    @Override
    public void downloadSongs(Song song, List<TorrentSongSource> sources) {
        final List<TorrentSongSource> mp3Sources = new ArrayList<>();
        final List<TorrentSongSource> flacSources = new ArrayList<>();

        for (TorrentSongSource source : sources) {
            if (source.isMp3()) {
                mp3Sources.add(source);
            } else {
                flacSources.add(source);
            }
        }

        final List<CompletableFuture<List<FileDocument>>> downloadingFiles = new ArrayList<>();
        if (!mp3Sources.isEmpty()) {
            downloadingFiles.addAll(downloadMp3Songs(song, mp3Sources));
        }
        if (!flacSources.isEmpty()) {
            downloadingFiles.addAll(downloadFlacSongs(song, flacSources));
        }

        for (CompletableFuture<List<FileDocument>> torrentFiles : downloadingFiles) {
            final List<FileDocument> fileDocuments;
            try {
                fileDocuments = torrentFiles.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            for (FileDocument fileDocument : fileDocuments) {
                logger.info("{} downloaded", fileDocument.getFsLocation());
            }
        }
    }

    private List<TorrentSongSource> searchSongSources(Song song, List<Future<TorrentDocument>> indexedTorrents) {
        final List<TorrentSongSource> songSources = new ArrayList<>();

        for (Future<TorrentDocument> indexedTorrent : indexedTorrents) {
            final TorrentDocument torrentDocument = awaitFutureNoError(indexedTorrent);

            if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_OK)) {
                final String torrentId = torrentDocument.getTorrentId();

                final String[] songTerms = ElasticUtils.asTerms3(song.getTitle());
                logger.info(String.format("SEARCH %s: %s (%s) in torrent %s", song.getArtist().getArtistName(), song.getTitle(), Arrays.asList(songTerms), torrentId));

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

    private List<CompletableFuture<List<FileDocument>>> downloadFlacSongs(Song song, List<TorrentSongSource> flacSources) {
        final List<CompletableFuture<List<FileDocument>>> downloadingFiles = new ArrayList<>();

        final Map<String, List<TorrentSongSource>> groupedFlacSources = groupPerTorrent(flacSources);
        final Set<String> flacTorrentIds = groupedFlacSources.keySet();

        flacTorrentIds.forEach(torrentId -> {
            final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

            final CompletableFuture<List<FileDocument>> fileDocumentFuture;
            fileDocumentFuture = CompletableFuture.supplyAsync(() -> {
                final List<FileDocument> results = new ArrayList<>();
                final List<TorrentSongSource> requestedSongSources = groupedFlacSources.get(torrentId);

                final File saveDir = new File(new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE), torrentId);
                if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                    saveDir.mkdirs();

                final List<String> requestedCues = getCueFiles(requestedSongSources);
                boolean allExist = isAllAlreadyDownloaded(saveDir, requestedCues);
                if (!allExist) {
                    // 1. resolve magnet
                    final String magnet = torrent.getTorrentInfo().getMagnet();
                    logger.info("resoling torrent {} info by magnet {}", torrentId, magnet);
                    final TorrentInfo torrentInfo = getByMagnet(magnet);

                    logger.info("[{}: {}] magnet resolved, downloading...", song.getArtist().getArtistName(), song.getTitle());

                    // 2. download
                    final Priority[] priorities = getAllFilesPriorities(torrentInfo, requestedSongSources);
                    appCoreService.getTorrentClient().download(torrentInfo, saveDir, priorities);
                    logger.info("downloading complete: {}", saveDir.getAbsolutePath());
                }

                // 3. flac -> mp3[]
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

                // 4. update resolved sources
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

                return results;
            });

            downloadingFiles.add(fileDocumentFuture);
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

    private List<CompletableFuture<List<FileDocument>>> downloadMp3Songs(Song song, List<TorrentSongSource> mp3Sources) {
        final List<CompletableFuture<List<FileDocument>>> downloadingFiles = new ArrayList<>();

        final Map<String, List<TorrentSongSource>> groupedMp3Sources = groupPerTorrent(mp3Sources);
        final Set<String> mp3TorrentIds = groupedMp3Sources.keySet();

        mp3TorrentIds.forEach(torrentId -> {
            final List<TorrentSongSource> torrentSources = groupedMp3Sources.get(torrentId);
            final List<TorrentSongSource> requiredSources = new ArrayList<>(torrentSources);

            final List<FileDocument> alreadyExistingFiles = filterAlreadyExisting(requiredSources);

            final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

            final CompletableFuture<List<FileDocument>> fileDocumentFuture;
            if (!requiredSources.isEmpty()) {
                fileDocumentFuture = CompletableFuture.supplyAsync(() -> {
                    final String magnet = torrent.getTorrentInfo().getMagnet();
                    logger.info("resoling torrent {} info by magnet {}", torrentId, magnet);
                    return getByMagnet(magnet);
                }, executor).thenApplyAsync(torrentInfo -> {
                    final List<FileDocument> fileDocuments = new ArrayList<>();

                    final Set<String> requiredFiles = getRequiredMp3FilePaths(requiredSources);

                    if (!requiredFiles.isEmpty()) {
                        logger.info("[{}: {}] magnet resolved, downloading '{}'", song.getArtist().getArtistName(), song.getTitle(), requiredFiles);

                        final Priority[] priorities = getRequiredFilesPriorities(torrentInfo, requiredFiles);

                        final File saveDir = new File(new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE), torrentId);
                        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                            saveDir.mkdirs();
                        appCoreService.getTorrentClient().download(torrentInfo, saveDir, priorities);

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


                    } else {
                        logger.warn("nothing to download (torrent {})", torrentId);
                    }

                    return fileDocuments;
                });
            } else {
                fileDocumentFuture = CompletableFuture.completedFuture(alreadyExistingFiles);
            }

            downloadingFiles.add(fileDocumentFuture);
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

    private void resolveArtistTorrents(Artist artist) {
        ArtistDocument artistDoc = dataStorage.getArtist(artist.getArtistId());
        if (artistDoc == null) {
            artistDoc = createArtistDocument(artist);
        }

        final String[] titleTerms = ElasticUtils.asTerms(artist.getArtistName());
        logger.info(String.format("ARTIST: %s (%s)", artist.getArtistName(), Arrays.asList(titleTerms)));

        for (Map.Entry<String, String> entry : forumFormats.entrySet()) {
            final String forumId = entry.getKey();
            final String format = entry.getValue();

            final Page<TorrentInfoVO> page = findTorrentsByTitle(new String[] {forumId}, titleTerms);

            if (page.getNumberOfElements() > 0) {
                logger.info("{} torrents found for {} ({})", page.getNumberOfElements(), artist.getArtistName(), format);
                for (int i = 0; i < page.getNumberOfElements(); i++) {
                    final TorrentInfoVO ti = page.getContent().get(i);

                    final TorrentDocument torrentDocument = createTorrentDocument(ti, format);
                    dataStorage.insertTorrent(torrentDocument);

                    artistDoc.getArtistTorrentIds().add(torrentDocument.getTorrentId());
                }
            } else {
                logger.warn("no any torrents found for {}", artist.getArtistName());
            }
        }

        if (artistDoc.getArtistTorrentIds().isEmpty()) {
            logger.warn("no torrents found for artist {} ({})", artist.getArtistName(), artist.getArtistId());
        }

        if (artistDoc.getId() == null) {
            dataStorage.insertArtist(artistDoc);
        } else {
            dataStorage.updateArtist(artistDoc);
        }
    }

    private ArtistDocument createArtistDocument(Artist artist) {
        ArtistDocument artistDoc;
        artistDoc = new ArtistDocument();
        artistDoc.setArtistId(artist.getArtistId());
        artistDoc.setArtistName(artist.getArtistName());
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


    private List<Future<TorrentDocument>> indexFlacTorrents(Song song) {
        logger.info("processing FLAC torrents...");

        final List<Future<TorrentDocument>> indexedTorrents = new ArrayList<>();

        // temp directory for FLAC mappings
        File saveDir = new File("C:\\TEMP\\find-music\\downloads\\flac\\");
        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
            saveDir.mkdirs();

        final ArtistEntity artistEntity = new ArtistEntity(song.getArtist().getArtistId(), song.getArtist().getArtistName());
        final ArtistDocument artistDocument = dataStorage.getArtist(song.getArtist().getArtistId());
        logger.info("{} torrents found", artistDocument.getArtistTorrentIds().size(), artistDocument.getArtistName());

        for (String torrentId : artistDocument.getArtistTorrentIds()) {
            logger.info("processing {} torrent...", torrentId);

            final TorrentDocument torrentDocument = dataStorage.getTorrent(torrentId);
            final TorrentInfoVO dumpTorrentInfo = torrentDocument.getTorrentInfo();

            if (!torrentDocument.getFormat().equals(MusicFormats.FORMAT_FLAC)) {
                logger.info("skipping torrent {} due to it's not FLAC torrent", torrentId);
            }

            if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_UNKNOWN)
                    || torrentDocument.getStatus().equals(ResolveStatuses.STATUS_ERROR)) {
                indexedTorrents.add(CompletableFuture.supplyAsync(() -> {

                    Boolean error = false;
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

                    return torrentDocument;
                }, executor));
            } else {
                logger.info("torrent {} is already indexed", torrentId);
                indexedTorrents.add(CompletableFuture.completedFuture(torrentDocument));
            }
        }

        return indexedTorrents;
    }


    private List<Future<TorrentDocument>> indexMp3Torrents(Song song) {
        logger.info("processing MP3 torrents...");

        final ArtistDocument artistDocument = dataStorage.getArtist(song.getArtist().getArtistId());
        logger.info("{} torrents found", artistDocument.getArtistTorrentIds().size(), artistDocument.getArtistName());

        final List<Future<TorrentDocument>> indexedTorrents = new ArrayList<>();

        for (String torrentId : artistDocument.getArtistTorrentIds()) {
            final TorrentDocument torrentDocument = dataStorage.getTorrent(torrentId);

            if (!torrentDocument.getFormat().equals(MusicFormats.FORMAT_MP3)) {
                logger.info("skipping torrent {} due to it's not MP3 torrent", torrentId);
            }

            if (torrentDocument.getStatus().equals(ResolveStatuses.STATUS_UNKNOWN)
                    || torrentDocument.getStatus().equals(ResolveStatuses.STATUS_ERROR)) {
                logger.info("indexing torrent {} (previous status {})", torrentId, torrentDocument.getStatus());

                indexedTorrents.add(
                        CompletableFuture.supplyAsync(() -> {
                            logger.info("fetching torrent {} info by magnet {}", torrentId, torrentDocument.getTorrentInfo().getMagnet());
                            final TorrentInfo byMagnet = getByMagnet(torrentDocument.getTorrentInfo().getMagnet());
                            if (byMagnet != null) {
                                logger.info("torrent {} info fetched", torrentId);
                            } else {
                                logger.warn("torrent {} info not found or timeout expired", torrentId);
                            }
                            return byMagnet;
                        }, executor)
                                .thenApply(torrentInfo -> {
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

                                    // update torrent information
                                    dataStorage.updateTorrent(torrentDocument);

                                    return torrentDocument;
                                })
                );
            } else {
                logger.info("torrent {} is already indexed", torrentId);
                indexedTorrents.add(CompletableFuture.completedFuture(torrentDocument));
            }
        }

        return indexedTorrents;
    }

    private TorrentDocument awaitFutureNoError(Future<TorrentDocument> indexedTorrent) {
        final TorrentDocument torrentDocument;
        try {
            torrentDocument = indexedTorrent.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return torrentDocument;
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

    private File downloadCue(TorrentInfo torrentInfo, Priority[] priorities, File targetFolder) {
        if (!targetFolder.exists()) //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
        appCoreService.getTorrentClient().download(torrentInfo, targetFolder, priorities);
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

                dataStorage.insertSongSource(songSourceDocument);
            }
        }
    }

}
