package ru.asm.core.dev.model.torrent;

import org.dizitart.no2.NitriteId;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;
import ru.asm.core.dev.model.Artist;
import ru.asm.core.dev.model.ddb.ArtistDocument;
import ru.asm.core.dev.model.ddb.DataStorage;
import ru.asm.core.dev.model.ddb.TorrentDocument;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.util.ElasticUtils;
import ru.asm.util.MusicFormats;
import ru.asm.util.ResolveStatuses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 12:04
 */
@Component
public class IndexedArtistsService {

    private static final Map<String, String> forumFormats = new HashMap<>();
    static {
        forumFormats.put("737", MusicFormats.FORMAT_FLAC); // Рок, Панк, Альтернатива (lossless)
        forumFormats.put("738", MusicFormats.FORMAT_MP3); // Рок, Панк, Альтернатива (lossy)
    }

    private static final Logger logger = LoggerFactory.getLogger(IndexedArtistsService.class);

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private TorrentInfoRepository torrentInfoRepository;

    public boolean isArtistIndexed(Integer artistId) {
        final ArtistDocument artist = dataStorage.getArtist(artistId);
        return artist != null && artist.getArtistTorrentIds() != null && !artist.getArtistTorrentIds().isEmpty();
    }

    public void indexArtist(Artist artist) {
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
                logger.info("{} torrents found for {}", page.getNumberOfElements(), artist.getArtistName());
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

        dataStorage.saveArtist(artistDoc);
    }

    private ArtistDocument createArtistDocument(Artist artist) {
        ArtistDocument artistDoc;
        artistDoc = new ArtistDocument();
        artistDoc.setId(NitriteId.newId().getIdValue());
        artistDoc.setArtistId(artist.getArtistId());
        artistDoc.setArtistName(artist.getArtistName());
        artistDoc.setArtistTorrentIds(new ArrayList<>());
        return artistDoc;
    }

    private TorrentDocument createTorrentDocument(TorrentInfoVO ti, String format) {
        final TorrentDocument torrentDocument = new TorrentDocument();
        torrentDocument.setId(NitriteId.newId().getIdValue());
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
}
