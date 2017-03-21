package ru.asm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import ru.asm.core.index.RepositoryAction;
import ru.asm.core.index.RepositoryTemplate;
import ru.asm.core.index.TorrentsDatabaseService;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.torrent.TorrentClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class ResolveArtistTorrentsTool {

    private static final Logger logger = LoggerFactory.getLogger(ResolveArtistTorrentsTool.class);

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        final PlaylistSongsMapper playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);

        final String playlist = "nashe";
        String[] forumIds = new String[]{
                "738"  // Рок, Панк, Альтернатива (lossy)
        };

        playlistSongsMapper.deleteAllArtistTorrent();
        final List<Integer> artistsIds = playlistSongsMapper.getAllArtistsIds();

        TorrentClient torrentClient = new TorrentClient();
        try {
            torrentClient.initializeSession();

            final ArrayList<String> found = new ArrayList<>();
            final ArrayList<String> notFound = new ArrayList<>();

            RepositoryTemplate template = new RepositoryTemplate(applicationContext);
            template.withRepository(new RepositoryAction() {
                @Override
                public void doAction(TorrentInfoRepository repo) {
                    for (Integer artistId : artistsIds) {
                        final String artist = playlistSongsMapper.getArtist(artistId);
                        resolveArtistTorrents(repo, artistId, artist, forumIds, found, notFound, playlistSongsMapper);
                    }
                }
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

    private static void resolveArtistTorrents(TorrentInfoRepository repo, Integer artistId, String artist, String[] forumIds, ArrayList<String> found, ArrayList<String> notFound, PlaylistSongsMapper playlistSongsMapper) {
        final String[] titleTerms = asTerms(artist);

        logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));
        TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(repo);

        Page<TorrentInfoVO> page = torrentsDatabaseService.findPage(forumIds, titleTerms, 0, 10);

        if (page.getNumberOfElements() > 0) {
            found.add(artist);
        } else {
            notFound.add(artist);
        }

        for (int i = 0; i < page.getNumberOfElements(); i++) {
            final TorrentInfoVO entry = page.getContent().get(i);
            logger.info("{}. [{}] {}", i + 1, entry.getForum(), entry.getTitle());

            playlistSongsMapper.insertArtistTorrent(artistId, entry.getId());
//                            try {
//                                TorrentInfo torrentInfo = torrentClient.findByMagnet(entry.getMagnet());
//                                // enlist files
//                                for (int j = 0; j < torrentInfo.files().numFiles(); j++) {
//                                    logger.info(torrentInfo.files().filePath(j));
//                                    // todo: put torrent files into index
//                                }
//                            } catch (TorrentClient.TorrentClientException e) {
//                                logger.info("FAILED: " + e.getErrCode());
//                            }
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
