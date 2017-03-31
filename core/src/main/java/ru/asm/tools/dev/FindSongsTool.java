package ru.asm.tools.dev;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import ru.asm.core.index.RepositoryTemplate;
import ru.asm.core.index.domain.TorrentFilesVO;
import ru.asm.core.index.repositories.TorrentFilesRepository;
import ru.asm.core.persistence.domain.PlaylistSongEntity;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class FindSongsTool {

    private static final Logger logger = LoggerFactory.getLogger(FindSongsTool.class);

    private ApplicationContext applicationContext;

    private PlaylistSongsMapper playlistSongsMapper;

    private TorrentFilesRepository torrentFilesRepository;

    public static void main(String[] args) {
        new FindSongsTool().resolveArtistTorrents();
    }


    public void resolveArtistTorrents() {
        applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);

        final List<PlaylistSongEntity> songs = playlistSongsMapper.getSongs();
//        for (PlaylistSongEntity song : songs) {
//            System.out.println(song);
//        }

        try {
            final ArrayList<PlaylistSongEntity> found = new ArrayList<>();
            final ArrayList<PlaylistSongEntity> notFound = new ArrayList<>();

            torrentFilesRepository = applicationContext.getBean(TorrentFilesRepository.class);
            new RepositoryTemplate<>(applicationContext, torrentFilesRepository).withRepository(torrentFilesRepo -> {
                for (PlaylistSongEntity song : songs) {
                    final String[] artistTerms = asTerms(song.getArtist());
                    final String[] songTerms = asTerms(song.getTitle());

                    logger.info(String.format("SEARCH %s (%s): %s (%s)", song.getArtist(), Arrays.asList(artistTerms), song.getTitle(), Arrays.asList(songTerms)));

                    Page<TorrentFilesVO> page = findPage(null, artistTerms, songTerms, 0, 10);
                    if (page.getNumberOfElements() > 0) {
                        found.add(song);
                        logger.info("{} torrents found", page.getTotalElements());

                        {
                            final List<TorrentFilesVO> content = page.getContent();
                            for (TorrentFilesVO torrentFilesVO : content) {
                                logger.info("- {} ({} files)", torrentFilesVO.getTorrentId(), torrentFilesVO.getTorrentSongs().size());
//                                for (TorrentSongVO torrentSongVO : torrentFilesVO.getTorrentSongs()) {
//                                    logger.info(" - " + torrentSongVO.getSongName());
//                                }
//
//                                for (String file : torrentFilesVO.getFileNames()) {
//                                    logger.info(" - " + file);
//                                }
                            }
                            //break;
                        }
                    } else {
                        notFound.add(song);
                        logger.warn("not found");
                    }
                }
            });


            logger.info("FOUND ({}): {}", found.size(), found);
            logger.warn("NOT FOUND ({}): {}", notFound.size(), notFound);
//            for (String s : notFound) {
//                System.out.println("\"" + s + "\"");
//            }
        } finally {
        }
    }

    public Page<TorrentFilesVO> findPage(String[] forumIdTerms, String[] artistTerms, String[] songTerms, int page, int pageSize) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (forumIdTerms != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("forumId", forumIdTerms));
        }
        if (artistTerms != null) {
            final BoolQueryBuilder builder = QueryBuilders.boolQuery();
            for (String term : artistTerms) {
                builder.should(QueryBuilders.wildcardQuery("artist", term));
            }
            builder.minimumNumberShouldMatch(artistTerms.length);

            boolQueryBuilder.must(builder);
        }

        if (songTerms != null) {
            final BoolQueryBuilder builder = QueryBuilders.boolQuery();
            for (String term : songTerms) {
                builder.should(QueryBuilders.wildcardQuery("torrentSongs.songName", term));
            }
            builder.minimumNumberShouldMatch(songTerms.length > 2 ? songTerms.length - 1 : songTerms.length);

            final NestedQueryBuilder torrentSongs = QueryBuilders.nestedQuery("torrentSongs", builder);

            boolQueryBuilder.must(torrentSongs);
        }

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(page, pageSize));
        searchQueryBuilder.withSort(new FieldSortBuilder("artist").order(SortOrder.ASC));

        Page<TorrentFilesVO> result = torrentFilesRepository.search(searchQueryBuilder.build());

        return result;
    }

    private static String[] asWildcards(String artist) {
        final String lowerCase = artist.toLowerCase();

        final String[] parts = lowerCase.replace("/", " ")
                .replace(".", "?")
                .replace("'", "?")
                .replace("-", "?")
                .replace(",", "?")
                .replace("+", "?")
                .split(";");

        final ArrayList<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                final String trimPart = part.trim();
                terms.add("*" + trimPart + "*");
            }
        }

        return terms.toArray(new String[terms.size()]);
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
