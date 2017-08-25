package ru.asm.tools;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentInfo;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.flac.FFileDescriptor;
import ru.asm.core.flac.FTrackDescriptor;
import ru.asm.core.index.RepositoryTemplate;
import ru.asm.core.index.TorrentsDatabaseService;
import ru.asm.core.index.domain.TorrentFilesVO;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentFilesRepository;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.torrent.TorrentClient;
import ru.asm.util.ElasticUtils;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class IndexArtistFlacTool {

    private static final Logger logger = LoggerFactory.getLogger(IndexArtistFlacTool.class);

    private ApplicationContext applicationContext;

    private PlaylistSongsMapper playlistSongsMapper;

    private TorrentClient torrentClient;

    private TorrentInfoRepository torrentInfoRepository;
    private TorrentFilesRepository torrentFilesRepository;

    private File saveDir;

    private static final int PARALLELISM = 8;
    private static final boolean DOWNLOAD_ALLOWED = true;
    private static final List<String> OVERRIDE_STATUSES = Arrays.asList(ResolveStatuses.STATUS_UNKNOWN, ResolveStatuses.STATUS_ERROR);
//    private static final boolean DOWNLOAD_ALLOWED = false;
//    private static final List<String> OVERRIDE_STATUSES = Arrays.asList(ResolveStatuses.STATUS_UNKNOWN, ResolveStatuses.STATUS_ERROR, ResolveStatuses.STATUS_OK);


    public static void main(String[] args) {
        new IndexArtistFlacTool().fillResolveArtistTorrentsTasks();
    }

    public void fillResolveArtistTorrentsTasks() {
        // init
        applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);
        torrentClient = new TorrentClient();

        this.torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class);
        this.torrentFilesRepository = applicationContext.getBean(TorrentFilesRepository.class);

        final TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(torrentInfoRepository);

        saveDir = new File("C:\\TEMP\\find-music\\downloads\\flac\\");
        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
            saveDir.mkdirs();

        final String forumId = "737";
        final ForkJoinPool forkJoinPool = new ForkJoinPool(PARALLELISM);

        // cleanup db log
//        playlistSongsMapper.deleteAllArtistTorrent(FORMAT_FLAC);
        final List<Integer> artistsIds = playlistSongsMapper.getAllArtistsIds();

        try {
            torrentClient.initializeSession();
            RepositoryTemplate.initializeRepos(applicationContext);

            final List<String> found = new ArrayList<>();
            final List<String> notFound = new ArrayList<>();

            {
                final List<ResolveSongsTask> tasks = new ArrayList<>();

                // prepare tasks to be executed
                for (Integer artistId : artistsIds) {
                    final String artist = playlistSongsMapper.getArtist(artistId);

                    final List<String> torrentIds = new ArrayList<>();
                    for (String overrideStatus : OVERRIDE_STATUSES) {
                        torrentIds.addAll(playlistSongsMapper.getArtistTorrents(artistId, MusicFormats.FORMAT_FLAC, overrideStatus));
                    }

                    final String[] titleTerms = ElasticUtils.asTerms2(artist);

                    logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));

                    if (!torrentIds.isEmpty()) {
                        found.add(artist);
                        for (String torrentId : torrentIds) {
                            final TorrentInfoVO dumpTorrentInfo = torrentsDatabaseService.findTorrent(torrentId);
                            if (dumpTorrentInfo == null) {
                                throw new AssertionError();
                            }

                            final ResolveSongsTask resolveSongsTask = new ResolveSongsTask(artist, artistId, dumpTorrentInfo, dumpTorrentInfo.getId(), () -> {
                                final File torrentDir = new File(saveDir, getFolderForTorrent(dumpTorrentInfo));

                                final Set<TorrentSongVO> torrentSongs = new HashSet<>();

                                //noinspection ConstantConditions
                                if (!isAlreadyDownloaded(torrentDir)) {

                                    // find torrent info by magnet
                                    final TorrentInfo torrentInfo = torrentClient.findByMagnet(dumpTorrentInfo.getMagnet());
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
                                    torrentSongs.addAll(getTorrentSongs(torrentDir));
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
                        playlistSongsMapper.updateArtistTorrentStatus(task.getArtistId(), MusicFormats.FORMAT_FLAC, task.getTorrentId(), ResolveStatuses.STATUS_OK);
                    } else {
                        final Throwable e = task.getError();
                        if (e != null) {
                            logger.error(e.getMessage());
                        }
                        logger.warn("[{}/{}] task failed", i, total);
                        playlistSongsMapper.updateArtistTorrentStatus(task.getArtistId(), MusicFormats.FORMAT_FLAC, task.getTorrentId(), ResolveStatuses.STATUS_ERROR);
                    }
                }
            }

            logger.info("FOUND ({}): {}", found.size(), found);
            logger.warn("NOT FOUND ({}): {}", notFound.size(), notFound);
        } finally {
            torrentClient.destroySession();
            RepositoryTemplate.destroyRepos(applicationContext);
        }
    }

    private boolean isAlreadyDownloaded(File torrentDir) {
        //noinspection ConstantConditions
        return torrentDir.exists() && torrentDir.list().length > 0;
    }

    private boolean isDownloadAllowed() {
        return DOWNLOAD_ALLOWED;
    }


    private Set<TorrentSongVO> getTorrentSongs(File torrentDir) throws IOException {
        final List<FFileDescriptor> fFileDescriptors = new ArrayList<>();
        final Iterator<File> fileIterator = FileUtils.iterateFiles(torrentDir, new String[]{"cue"}, true);
        while (fileIterator.hasNext()) {
            final File cueFile = fileIterator.next();
            fFileDescriptors.addAll(new CueParser().parseCue(torrentDir, cueFile));
        }
        final Set<TorrentSongVO> torrentSongs = new HashSet<>();
        for (FFileDescriptor fFileDescriptor : fFileDescriptors) {
            for (FTrackDescriptor fTrackDescriptor : fFileDescriptor.getTrackDescriptors()) {
                torrentSongs.add(new TorrentSongVO(fTrackDescriptor.getTitle(),
                        fFileDescriptor.getRelativePath(), fFileDescriptor.getRelativePath(),
                        fTrackDescriptor.getTrackNum(), fTrackDescriptor.getIndexTime()));
            }
        }
        return torrentSongs;
    }

    private File downloadCue(TorrentInfo torrentInfo, Priority[] priorities, File targetFolder) {
        if (!targetFolder.exists()) //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
        torrentClient.download(torrentInfo, targetFolder, priorities);
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
}
