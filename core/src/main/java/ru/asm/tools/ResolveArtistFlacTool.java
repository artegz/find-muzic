package ru.asm.tools;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentInfo;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class ResolveArtistFlacTool {

    private static final Logger logger = LoggerFactory.getLogger(ResolveArtistFlacTool.class);
    public static final Charset ENCODING = Charset.forName("windows-1251");
    public static final String FORMAT_FLAC = "FLAC";
    public static final String STATUS_INTERNAL_ERROR = "ERROR";
    public static final String STATUS_NOT_FOUND_OR_TIMEOUT_EXPIRED = "NULL";
    public static final String STATUS_OK = "OK";

    private ApplicationContext applicationContext;

    private PlaylistSongsMapper playlistSongsMapper;

    private TorrentClient torrentClient;

    private TorrentInfoRepository torrentInfoRepository;
    private TorrentFilesRepository torrentFilesRepository;

    private File saveDir;

    public static void main(String[] args) {
        new ResolveArtistFlacTool().fillResolveArtistTorrentsTasks();
    }

    public void fillResolveArtistTorrentsTasks() {
        // init
        applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);
        torrentClient = new TorrentClient();
        saveDir = new File("C:\\TEMP\\find-music\\downloads\\flac\\");
        if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
            saveDir.mkdirs();
        this.torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class);
        this.torrentFilesRepository = applicationContext.getBean(TorrentFilesRepository.class);
        final String forumId = "737";
        final ForkJoinPool forkJoinPool = new ForkJoinPool(8);

        // cleanup db log
//        playlistSongsMapper.deleteAllArtistTorrent(FORMAT_FLAC);
        final List<Integer> artistsIds = playlistSongsMapper.getAllArtistsIds();

        try {
            torrentClient.initializeSession();

            final List<String> found = new ArrayList<>();
            final List<String> notFound = new ArrayList<>();

            final RepositoryTemplate<TorrentInfoVO, String> torrentInfoRepositoryTemplate = new RepositoryTemplate<>(applicationContext, torrentInfoRepository);
            final RepositoryTemplate<TorrentFilesVO, String> torrentFilesRepositoryTemplate = new RepositoryTemplate<>(applicationContext, torrentFilesRepository);
            torrentInfoRepositoryTemplate.withRepository(torrentsInfoRepo -> torrentFilesRepositoryTemplate.withRepository(torrentFilesRepo -> {
                final List<ResolveTask> tasks = new ArrayList<>();

                // prepare tasks to be executed
                for (Integer artistId : artistsIds) {
                    final String artist = playlistSongsMapper.getArtist(artistId);
                    fillResolveArtistTorrentsTasks(artistId, artist, found, notFound, tasks, forumId);
                }

                // submit all tasks
                for (ResolveTask task : tasks) {
                    forkJoinPool.submit(task);
                }

                // join all tasks
                int i = 0;
                for (ResolveTask task : tasks) {
                    i++;
                    final TorrentFilesVO res = task.join();
                    final Integer artistId = task.getArtistId();
                    if (res == null) {
                        logger.warn("task {} failed", i);
                        final Throwable e = task.getError();
                        dbLogFailed(forumId, artistId, task.getTorrentId(), e);
                    } else {
                        logger.info("task {} completed", i);
                        // index found torrent songs
                        torrentFilesRepo.index(res);
                        dbLobSucceeded(forumId, artistId, task.getTorrentId());
                    }
                }
            }));

            logger.info("FOUND ({}): {}", found.size(), found);
            logger.warn("NOT FOUND ({}): {}", notFound.size(), notFound);
//            for (String s : notFound) {
//                System.out.println("\"" + s + "\"");
//            }
        } finally {
            torrentClient.destroySession();
        }
    }

    private void dbLobSucceeded(String forumId, Integer artistId, String torrentId) {
        // add found artist torrent
        playlistSongsMapper.insertArtistTorrent(artistId, torrentId, FORMAT_FLAC, forumId, STATUS_OK);
    }

    private void dbLogFailed(String forumId, Integer artistId, String torrentId, Throwable e) {
        if (e != null) {
            logger.error(e.getMessage());
            if (e instanceof TorrentClient.TorrentClientException) {
                final String errCode = ((TorrentClient.TorrentClientException) e).getErrCode();
                playlistSongsMapper.insertArtistTorrent(artistId, torrentId, FORMAT_FLAC, forumId, errCode);
            } else {
                playlistSongsMapper.insertArtistTorrent(artistId, torrentId, FORMAT_FLAC, forumId, STATUS_INTERNAL_ERROR);
            }
        } else {
            playlistSongsMapper.insertArtistTorrent(artistId, torrentId, FORMAT_FLAC, forumId, STATUS_NOT_FOUND_OR_TIMEOUT_EXPIRED);
        }
    }

    private void fillResolveArtistTorrentsTasks(Integer artistId,
                                                String artist,
                                                List<String> found,
                                                List<String> notFound,
                                                List<ResolveTask> tasks,
                                                String forumId) {
        TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(torrentInfoRepository);

        final String[] titleTerms = asTerms(artist);

        logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));

        Page<TorrentInfoVO> page = torrentsDatabaseService.findPage(new String[] {forumId}, titleTerms, 0, 50);
        if (page.getNumberOfElements() > 0) {
            found.add(artist);

            for (int i = 0; i < page.getNumberOfElements(); i++) {
                final TorrentInfoVO entry = page.getContent().get(i);

                final int index = i + 1;
                final ResolveTask resolveTask = new ResolveTask(artist, artistId, entry, entry.getId(), () -> {
                    logger.info("{}. [{}] {}", index, entry.getForum(), entry.getTitle());

                    // find torrent
                    final TorrentInfo torrentInfo = torrentClient.findByMagnet(entry.getMagnet());
                    if (torrentInfo == null) {
                        logger.warn("not found or timeout expired");
                        return null;
                    }

                    // set priority for .cue files
                    final List<String> cueFilePaths = new ArrayList<>();
                    final Priority[] priorities = ignoreAllExceptCuePriorities(torrentInfo, cueFilePaths);

                    // download cue files
                    final File torrentDir = downloadCue(torrentInfo, priorities);

                    // parse cue files (find contained songs)
                    final List<FFileDescriptor> fFileDescriptors = new ArrayList<>();
                    final Iterator<File> fileIterator = FileUtils.iterateFiles(torrentDir, new String[]{"cue"}, true);
                    while (fileIterator.hasNext()) {
                        final File cueFile = fileIterator.next();
                        fFileDescriptors.addAll(parseCue(torrentDir, cueFile));
                    }
                    final Set<TorrentSongVO> torrentSongs = new HashSet<>();
                    for (FFileDescriptor fFileDescriptor : fFileDescriptors) {
                        for (FTrackDescriptor fTrackDescriptor : fFileDescriptor.getTrackDescriptors()) {
                            torrentSongs.add(new TorrentSongVO(fFileDescriptor.getTitle(),
                                    fFileDescriptor.getRelativePath(), fFileDescriptor.getRelativePath(),
                                    fTrackDescriptor.getTrackNum(), fTrackDescriptor.getIndexTime()));
                        }
                    }

                    // print found songs
//                    for (FFileDescriptor fFileDescriptor : fFileDescriptors) {
//                        System.out.println(fFileDescriptor.getFile());
//                        for (FTrackDescriptor trackDescriptor : fFileDescriptor.getTrackDescriptors()) {
//                            System.out.println(" - " + trackDescriptor.getTitle());
//                        }
//                    }

                    final TorrentFilesVO torrentFilesVO = new TorrentFilesVO();
                    torrentFilesVO.setTorrentId(entry.getId());
                    torrentFilesVO.setArtist(artist);
                    torrentFilesVO.setArtistId(artistId);
                    torrentFilesVO.setForumId(forumId);
                    torrentFilesVO.setMagnet(entry.getMagnet());
                    torrentFilesVO.setTorrentSongs(new ArrayList<>(torrentSongs));

                    return torrentFilesVO;
                });
                tasks.add(resolveTask);
            }
        } else {
            notFound.add(artist);
        }

        logger.info("total: {}", page.getTotalElements());
    }

    private File downloadCue(TorrentInfo torrentInfo, Priority[] priorities) {
        final File saveDir = this.saveDir;
        final File torrentDir = new File(saveDir, torrentInfo.name());
        if (!torrentDir.exists()) //noinspection ResultOfMethodCallIgnored
            torrentDir.mkdirs();
        torrentClient.download(torrentInfo, torrentDir, priorities);
        return torrentDir;
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

    private List<FFileDescriptor> parseCue(File torrentDir, File cueFile) throws IOException {
        // e.g. C:\download\group\album\1.cue
        final String cueFilePath = cueFile.getAbsolutePath();
        // e.g. C:\download\group\
        final String downloadPath = torrentDir.getAbsolutePath();
        final String relativePath = cueFilePath.replace(downloadPath, "");

        final List<String> fileLines = FileUtils.readLines(cueFile, ENCODING);
        final List<String> lines = new ArrayList<>(fileLines);

        final List<FFileDescriptor> fFileDescriptors = new ArrayList<>();

        String performer = null;
        String title = null;

        String trackNum = null;
        String trackType = null;

        String indexNum = null;
        String indexTime = null;

        FFileDescriptor ffileDescriptor = null;
        FTrackDescriptor ftrackDescriptor = null;

        for (String line : lines) {
            final String trimLine = line.trim();
            if (trimLine.startsWith("PERFORMER")) {
                performer = trimLine.split(" ")[1];
            } else if (trimLine.startsWith("TITLE")) {
                title = trimLine.replaceAll("TITLE \"(.*)\"", "$1");
            } else if (trimLine.startsWith("FILE")) {
                String file = trimLine.replaceAll("FILE \"(.*)\" .*", "$1");
                String fileType = trimLine.replaceAll("FILE \".*\" (.*)", "$1");

                ffileDescriptor = new FFileDescriptor(relativePath, cueFile.getName());
                ffileDescriptor.setFile(file);
                ffileDescriptor.setFileType(fileType);
                ffileDescriptor.setTitle(title);
                ffileDescriptor.setPerformer(performer);
                ffileDescriptor.setTrackDescriptors(new ArrayList<>());

                fFileDescriptors.add(ffileDescriptor);

                title = null;
                performer = null;
            } else if (trimLine.startsWith("INDEX")) {
                indexNum = trimLine.split(" ")[1];
                indexTime = trimLine.split(" ")[2];
            } else if (trimLine.startsWith("TRACK")) {
                if (ftrackDescriptor != null) {
                    ftrackDescriptor.setTrackNum(trackNum);
                    ftrackDescriptor.setTrackType(trackType);
                    ftrackDescriptor.setTitle(title);
                    ftrackDescriptor.setPerformer(performer);
                    ftrackDescriptor.setIndexNum(indexNum);
                    ftrackDescriptor.setIndexTime(indexTime);

                    ffileDescriptor.getTrackDescriptors().add(ftrackDescriptor);
                }

                ftrackDescriptor = new FTrackDescriptor();
                trackNum = trimLine.split(" ")[1];
                trackType = trimLine.split(" ")[2];
                title = null;
                performer = null;
                indexNum = null;
                indexTime = null;
            }
        }

        if (ftrackDescriptor != null) {
            ftrackDescriptor.setTrackNum(trackNum);
            ftrackDescriptor.setTrackType(trackType);
            ftrackDescriptor.setTitle(title);
            ftrackDescriptor.setPerformer(performer);
            ftrackDescriptor.setIndexNum(indexNum);
            ftrackDescriptor.setIndexTime(indexTime);

            ffileDescriptor.getTrackDescriptors().add(ftrackDescriptor);
            ftrackDescriptor = null;
        }

        return fFileDescriptors;
    }

    private Priority[] getPriorities(int numFiles) {
        final Priority[] priorities = new Priority[numFiles];
        for (int j = 0; j < numFiles; j++) {
            priorities[j] = Priority.IGNORE;
        }
        return priorities;
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

    private static class ResolveTask extends ForkJoinTask<TorrentFilesVO> {

        private TorrentFilesVO result = null;

        private String artist;
        private Integer artistId;
        private TorrentInfoVO torrentInfo;

        private String torrentId;

        private Callable<TorrentFilesVO> callable;

        private Throwable error;

        public ResolveTask(String artist, Integer artistId, TorrentInfoVO torrentInfo, String torrentId, Callable<TorrentFilesVO> callable) {
            this.artist = artist;
            this.artistId = artistId;
            this.torrentInfo = torrentInfo;
            this.torrentId = torrentId;
            this.callable = callable;
        }

        public String getArtist() {
            return artist;
        }

        public Integer getArtistId() {
            return artistId;
        }

        public TorrentInfoVO getTorrentInfo() {
            return torrentInfo;
        }

        public String getTorrentId() {
            return torrentId;
        }

        @Override
        public TorrentFilesVO getRawResult() {
            return result;
        }

        public Throwable getError() {
            return error;
        }

        @Override
        protected void setRawResult(TorrentFilesVO value) {
            result = value;
        }

        @Override
        protected boolean exec() {
            try {
                final TorrentFilesVO res = callable.call();
                setRawResult(res);
                return true;
            } catch (Throwable e) {
                error = e;
                return true;
            }
        }
    }
}
