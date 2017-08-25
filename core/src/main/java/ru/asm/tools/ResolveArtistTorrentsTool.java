package ru.asm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import ru.asm.core.index.RepositoryTemplate;
import ru.asm.core.index.TorrentsDatabaseService;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.util.ElasticUtils;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import java.util.*;

/**
 * User: artem.smirnov
 * Date: 31.03.2017
 * Time: 12:06
 */
public class ResolveArtistTorrentsTool {

    private static final Logger logger = LoggerFactory.getLogger(IndexArtistFlacTool.class);

    private ApplicationContext applicationContext;

    private PlaylistSongsMapper playlistSongsMapper;


    public static void main(String[] args) {
        new ResolveArtistTorrentsTool().resolve();
    }

    public void resolve() {
        // init
        applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);

        final List<String> found = new ArrayList<>();
        final List<String> notFound = new ArrayList<>();

        final Map<String, String> forumFormats = new HashMap<>();
        forumFormats.put("737", MusicFormats.FORMAT_FLAC); // Рок, Панк, Альтернатива (lossless)
        forumFormats.put("738", MusicFormats.FORMAT_MP3); // Рок, Панк, Альтернатива (lossy)

        final TorrentInfoRepository torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class);

        final RepositoryTemplate<TorrentInfoVO, String> torrentInfoRepositoryTemplate = new RepositoryTemplate<>(applicationContext, torrentInfoRepository);
        torrentInfoRepositoryTemplate.withRepository(torrentsInfoRepo -> {
            final List<Integer> artistsIds = playlistSongsMapper.getAllArtistsIds();
            final TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(torrentInfoRepository);

            // prepare tasks to be executed
            for (Integer artistId : artistsIds) {
                final String artist = playlistSongsMapper.getArtist(artistId);
                final String[] titleTerms = ElasticUtils.asTerms(artist);

                logger.info(String.format("ARTIST: %s (%s)", artist, Arrays.asList(titleTerms)));

                for (Map.Entry<String, String> entry : forumFormats.entrySet()) {
                    final String forumId = entry.getKey();
                    final String format = entry.getValue();

                    final Page<TorrentInfoVO> page = torrentsDatabaseService.findPage(new String[] {forumId}, titleTerms, 0, 50);

                    if (page.getNumberOfElements() > 0) {
                        found.add(artist);

                        for (int i = 0; i < page.getNumberOfElements(); i++) {
                            final TorrentInfoVO ti = page.getContent().get(i);
                            playlistSongsMapper.insertArtistTorrent(artistId, ti.getId(), format, forumId, ResolveStatuses.STATUS_UNKNOWN);
                        }
                    } else {
                        notFound.add(artist);
                    }

                    logger.info("total: {} {}", page.getTotalElements(), format);
                }

            }
        });
    }
}


