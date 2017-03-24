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
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.repositories.TorrentFilesRepository;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.torrent.TorrentClient;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class ResolveArtistFlacTool {

    private static final Logger logger = LoggerFactory.getLogger(ResolveArtistFlacTool.class);
    public static final Charset ENCODING = Charset.forName("windows-1251");

    private ApplicationContext applicationContext;

    private PlaylistSongsMapper playlistSongsMapper;

    private TorrentClient torrentClient;
    private File saveDir;

    public static void main(String[] args) {
        new ResolveArtistFlacTool().resolveArtistTorrents();
    }



    public void resolveArtistTorrents() {
        applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);
        torrentClient = new TorrentClient();
        saveDir = new File("C:\\TEMP\\find-music\\downloads\\flac\\");
        if (!saveDir.exists()) saveDir.mkdirs();

//        playlistSongsMapper.deleteAllArtistTorrent();
        final List<Integer> artistsIds = playlistSongsMapper.getAllArtistsIds();

        try {
            torrentClient.initializeSession();

            final ArrayList<String> found = new ArrayList<>();
            final ArrayList<String> notFound = new ArrayList<>();

            final TorrentInfoRepository torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class);
            final TorrentFilesRepository torrentFilesRepository = applicationContext.getBean(TorrentFilesRepository.class);

            new RepositoryTemplate<>(applicationContext, torrentInfoRepository).withRepository(torrentsInfoRepo -> {
                new RepositoryTemplate<>(applicationContext, torrentFilesRepository).withRepository(torrentFilesRepo -> {
                    for (Integer artistId : artistsIds) {
                        final String artist = playlistSongsMapper.getArtist(artistId);
                        resolveArtistTorrents(((TorrentInfoRepository) torrentsInfoRepo), ((TorrentFilesRepository) torrentFilesRepo), artistId, artist, found, notFound);
                    }
                });
            });


            logger.info("FOUND ({}): {}", found.size(), found);
            logger.warn("NOT FOUND ({}): {}", notFound.size(), notFound);
//            for (String s : notFound) {
//                System.out.println("\"" + s + "\"");
//            }
        } finally {
            torrentClient.destroySession();
        }
    }

    private void resolveArtistTorrents(TorrentInfoRepository torrentsInfoRepo,
                                       TorrentFilesRepository torrentFilesRepo,
                                       Integer artistId,
                                       String artist,
                                       ArrayList<String> found,
                                       ArrayList<String> notFound) {
        TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(torrentsInfoRepo);

        final String[] titleTerms = asTerms(artist);
        final String[] forumIds = new String[]{
                //"738",  // Рок, Панк, Альтернатива (lossy)
                "737" // Рок, Панк, Альтернатива (lossless)
        };

        logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));

        Page<TorrentInfoVO> page = torrentsDatabaseService.findPage(forumIds, titleTerms, 0, 10);
        if (page.getNumberOfElements() > 0) {
            found.add(artist);

            for (int i = 0; i < page.getNumberOfElements(); i++) {
                final TorrentInfoVO entry = page.getContent().get(i);
                logger.info("{}. [{}] {}", i + 1, entry.getForum(), entry.getTitle());

//                final List<String> fileNames = new ArrayList<>();
                try {
                    // find torrent
                    final TorrentInfo torrentInfo = torrentClient.findByMagnet(entry.getMagnet());

                    // set priority for .cue files
                    final Priority[] priorities = ignoreAllExceptCuePriorities(torrentInfo);

                    // download cue files
                    final File torrentDir = downloadCue(torrentInfo, priorities);

                    final List<FFileDescriptor> fFileDescriptors = new ArrayList<>();
                    final Iterator<File> fileIterator = FileUtils.iterateFiles(torrentDir, new String[]{"cue"}, true);
                    while (fileIterator.hasNext()) {
                        final File cueFile = fileIterator.next();
                        fFileDescriptors.addAll(parseCue(cueFile));
                    }

                    for (FFileDescriptor fFileDescriptor : fFileDescriptors) {
                        System.out.println(fFileDescriptor.getFile());
                        for (FTrackDescriptor trackDescriptor : fFileDescriptor.getTrackDescriptors()) {
                            System.out.println(" - " + trackDescriptor.getTitle());
                        }
                    }


                    throw new StoppedException("stopped");

//                    final TorrentFilesVO torrentFilesVO = new TorrentFilesVO();
//                    torrentFilesVO.setTorrentId(entry.getId());
//                    torrentFilesVO.setArtist(artist);
//                    torrentFilesVO.setArtistId(artistId);
//                    torrentFilesVO.setForumId(forumIds[0]);
//                    torrentFilesVO.setMagnet(entry.getMagnet());
//                    torrentFilesVO.setFileNames(fileNames);
//                    // todo: add paths, entry.getTitle
//                    // todo: increase timeout, use fork-join
//
//                    torrentFilesRepo.index(torrentFilesVO);
//
//                    playlistSongsMapper.insertArtistTorrent(artistId, entry.getId(), forumIds[0], "OK");

                } catch (StoppedException e) {
                    throw e;
                } catch (TorrentClient.TorrentClientException e) {
                    logger.error(e.getMessage());
//                    playlistSongsMapper.insertArtistTorrent(artistId, entry.getId(), forumIds[0], e.getErrCode());
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
//                    playlistSongsMapper.insertArtistTorrent(artistId, entry.getId(), forumIds[0], "ERROR");
                }
            }


        } else {
            notFound.add(artist);
        }

        logger.info("total: {}", page.getTotalElements());
    }

    private File downloadCue(TorrentInfo torrentInfo, Priority[] priorities) {
        final File saveDir = this.saveDir;
        final File torrentDir = new File(saveDir, torrentInfo.name());
        if (!torrentDir.exists()) torrentDir.mkdirs();
        torrentClient.download(torrentInfo, torrentDir, priorities);
        return torrentDir;
    }

    private Priority[] ignoreAllExceptCuePriorities(TorrentInfo torrentInfo) {
        // enlist files
        final int numFiles = torrentInfo.files().numFiles();
        final Priority[] priorities = getPriorities(numFiles);
        for (int j = 0; j < numFiles; j++) {
            final FileStorage fileStorage = torrentInfo.files();
            final String filePath = fileStorage.filePath(j);
            final long fileSize = fileStorage.fileSize(j);
            final String fileName = fileStorage.fileName(j);

            logger.info("{} ({})", filePath, fileSize);

            if (fileName.endsWith(".cue")) {
                priorities[j] = Priority.NORMAL;
            }
//                        final String fileName = fileStorage.fileName(j);
//                        if (fileName.endsWith(".mp3")) {
//                            fileNames.add(fileName);
//                        }
        }
        return priorities;
    }

    private ArrayList<FFileDescriptor> parseCue(File cueFile) throws IOException {
        final List<String> fileLines = FileUtils.readLines(cueFile, ENCODING);

        final List<String> lines = new ArrayList<>(fileLines);

        final ArrayList<FFileDescriptor> fFileDescriptors = new ArrayList<>();

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

                ffileDescriptor = new FFileDescriptor();
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

    private static class StoppedException extends RuntimeException {


        public StoppedException(String message) {
            super(message);
        }
    }
}
