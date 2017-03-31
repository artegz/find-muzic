package ru.asm.tools;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.TorrentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import ru.asm.core.index.RepositoryTemplate;
import ru.asm.core.index.TorrentsDatabaseService;
import ru.asm.core.index.domain.TorrentFilesVO;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentFilesRepository;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.torrent.TorrentClient;

import java.util.*;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class ResolveArtistMp3Tool {

    private static final Logger logger = LoggerFactory.getLogger(ResolveArtistMp3Tool.class);
    public static final String FORMAT_MP3 = "MP3";

    private ApplicationContext applicationContext;

    private PlaylistSongsMapper playlistSongsMapper;

    private TorrentClient torrentClient;

    public static void main(String[] args) {
        new ResolveArtistMp3Tool().resolveArtistTorrents();
    }


    public void resolveArtistTorrents() {
        applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);
        torrentClient = new TorrentClient();

        playlistSongsMapper.deleteAllArtistTorrent(FORMAT_MP3);
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
        final String forumId = "738"; // // Рок, Панк, Альтернатива (lossy)

        logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));

        Page<TorrentInfoVO> page = torrentsDatabaseService.findPage(new String[]{forumId}, titleTerms, 0, 50);
        if (page.getNumberOfElements() > 0) {
            found.add(artist);
        } else {
            notFound.add(artist);
        }

        for (int i = 0; i < page.getNumberOfElements(); i++) {
            final TorrentInfoVO entry = page.getContent().get(i);
            logger.info("{}. [{}] {}", i + 1, entry.getForum(), entry.getTitle());

            try {
                TorrentInfo torrentInfo = torrentClient.findByMagnet(entry.getMagnet());
                if (torrentInfo == null) {
                    logger.warn("not found or timeout expired");
                    continue;
                }

                // enlist files
                final Set<TorrentSongVO> torrentSongs = new HashSet<>();
                for (int j = 0; j < torrentInfo.files().numFiles(); j++) {
                    final FileStorage fileStorage = torrentInfo.files();
                    final String filePath = fileStorage.filePath(j);
                    final String fileName = fileStorage.fileName(j);

                    if (fileName.toLowerCase().endsWith(".mp3")) {
                        final String title = fileName.replace(".mp3", "").replace(".MP3", "");
                        torrentSongs.add(new TorrentSongVO(title, fileName, filePath));
                    }
                    logger.info(filePath);
                }

                final TorrentFilesVO torrentFilesVO = new TorrentFilesVO();
                torrentFilesVO.setTorrentId(entry.getId());
                torrentFilesVO.setArtist(artist);
                torrentFilesVO.setArtistId(artistId);
                torrentFilesVO.setForumId(forumId);
                torrentFilesVO.setMagnet(entry.getMagnet());
                torrentFilesVO.setTorrentSongs(new ArrayList<>(torrentSongs));

                torrentFilesRepo.index(torrentFilesVO);

                playlistSongsMapper.insertArtistTorrent(artistId, entry.getId(), FORMAT_MP3, forumId, "OK");
            } catch (TorrentClient.TorrentClientException e) {
                logger.error(e.getMessage());
                playlistSongsMapper.insertArtistTorrent(artistId, entry.getId(), FORMAT_MP3, forumId, e.getErrCode());
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                playlistSongsMapper.insertArtistTorrent(artistId, entry.getId(), FORMAT_MP3, forumId, "ERROR");
            }
        }
        logger.info("total: {}", page.getTotalElements());
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
}
