package ru.asm.tools;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.TorrentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.index.RepositoryTemplate;
import ru.asm.core.index.TorrentsDatabaseService;
import ru.asm.core.index.domain.TorrentFilesVO;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentFilesRepository;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.torrent.TorrentClient;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class IndexArtistMp3Tool {

    private static final Logger logger = LoggerFactory.getLogger(IndexArtistMp3Tool.class);


    private static final int PARALLELISM = 8;
    private static final boolean DOWNLOAD_ALLOWED = true;
    private static final List<String> OVERRIDE_STATUSES = Arrays.asList(ResolveStatuses.STATUS_UNKNOWN, ResolveStatuses.STATUS_ERROR);


    private ApplicationContext applicationContext;

    private PlaylistSongsMapper playlistSongsMapper;

    private TorrentClient torrentClient;

    public static void main(String[] args) {
        new IndexArtistMp3Tool().resolveArtistTorrents();
    }


    public void resolveArtistTorrents() {
        applicationContext = new AnnotationConfigApplicationContext("ru.asm");

        playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);
        final TorrentInfoRepository torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class);
        final TorrentFilesRepository torrentFilesRepository = applicationContext.getBean(TorrentFilesRepository.class);

        torrentClient = new TorrentClient();

        final ForkJoinPool forkJoinPool = new ForkJoinPool(PARALLELISM);

        try {
            torrentClient.initializeSession();
            RepositoryTemplate.initializeRepos(applicationContext);

            final ArrayList<String> found = new ArrayList<>();
            final ArrayList<String> notFound = new ArrayList<>();


            // todo asm: add parallelism; save downloaded torrent infos (at least files list)
            {
                final List<Integer> artistsIds = playlistSongsMapper.getAllArtistsIds();

                for (Integer artistId : artistsIds) {
                    final TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(torrentInfoRepository);

                    final List<String> torrentIds = new ArrayList<>();
                    torrentIds.addAll(playlistSongsMapper.getArtistTorrents(artistId, MusicFormats.FORMAT_MP3, ResolveStatuses.STATUS_UNKNOWN));
                    torrentIds.addAll(playlistSongsMapper.getArtistTorrents(artistId, MusicFormats.FORMAT_MP3, ResolveStatuses.STATUS_ERROR));

                    final String artist = playlistSongsMapper.getArtist(artistId);
                    final String[] titleTerms = asTerms(artist);

                    logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));


                    if (!torrentIds.isEmpty()) {
                        found.add(artist);
                        for (String torrentId : torrentIds) {
                            final TorrentInfoVO torrent = torrentsDatabaseService.findTorrent(torrentId);
                            if (torrent == null) {
                                throw new AssertionError();
                            }

                            try {
                                TorrentInfo torrentInfo = torrentClient.findByMagnet(torrent.getMagnet());
                                if (torrentInfo == null) {
                                    playlistSongsMapper.updateArtistTorrentStatus(artistId, MusicFormats.FORMAT_MP3, torrent.getId(), ResolveStatuses.STATUS_ERROR);
                                    continue;
                                }

                                final TorrentFilesVO torrentFilesVO = new TorrentFilesVO();
                                torrentFilesVO.setTorrentId(torrent.getId());
                                torrentFilesVO.setArtist(artist);
                                torrentFilesVO.setArtistId(artistId);
                                torrentFilesVO.setForumId(torrent.getForumId());
                                torrentFilesVO.setMagnet(torrent.getMagnet());
                                torrentFilesVO.setTorrentSongs(new ArrayList<>(getTorrentSongs(torrentInfo)));

                                torrentFilesRepository.index(torrentFilesVO);

                                playlistSongsMapper.updateArtistTorrentStatus(artistId, MusicFormats.FORMAT_MP3, torrent.getId(), ResolveStatuses.STATUS_OK);
                            } catch (Throwable e) {
                                logger.error(e.getMessage(), e);
                                playlistSongsMapper.updateArtistTorrentStatus(artistId, MusicFormats.FORMAT_MP3, torrent.getId(), ResolveStatuses.STATUS_ERROR);
                            }
                        }
                    } else {
                        notFound.add(artist);
                    }

                    logger.info("total: {}", torrentIds.size());
                }
            };


            logger.info("FOUND ({}): {}", found.size(), found);
            logger.warn("NOT FOUND ({}): {}", notFound.size(), notFound);
//            for (String s : notFound) {
//                System.out.println("\"" + s + "\"");
//            }
        } finally {
            torrentClient.destroySession();
            RepositoryTemplate.destroyRepos(applicationContext);
        }
    }

    private Set<TorrentSongVO> getTorrentSongs(TorrentInfo torrentInfo) {
        final Set<TorrentSongVO> torrentSongs = new HashSet<>();
        for (int j = 0; j < torrentInfo.files().numFiles(); j++) {
            final FileStorage fileStorage = torrentInfo.files();
            final String filePath = fileStorage.filePath(j);
            final String fileName = fileStorage.fileName(j);

            if (fileName.toLowerCase().endsWith(".mp3")) {
                final String title = fileName.replace(".mp3", "").replace(".MP3", "");
                torrentSongs.add(new TorrentSongVO(title, fileName, filePath));
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
}
