package ru.asm.core.index;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.util.ElasticUtils;

/**
 * User: artem.smirnov
 * Date: 22.06.2016
 * Time: 9:26
 */
@Component
public class TorrentsDatabaseService {

    @Autowired
    private TorrentInfoRepository torrentInfoRepository;

    TorrentsDatabaseService() {
    }

    public TorrentsDatabaseService(TorrentInfoRepository torrentInfoRepository) {
        this.torrentInfoRepository = torrentInfoRepository;
    }

    public void printTotalCount() {
        Long count = torrentInfoRepository.count();
        System.out.printf("%d records%n", count);
    }

    public void findAndPrint(boolean regexp, String mainCategoryQuery, String subCategoryQuery, String foldersQuery, String titleQuery, Integer page) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (mainCategoryQuery != null) {
            if (!regexp) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("mainCategory", mainCategoryQuery));
            } else {
                boolQueryBuilder.must(QueryBuilders.regexpQuery("mainCategory", mainCategoryQuery));
            }
        }
        if (subCategoryQuery != null) {
            if (!regexp) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("subCategory", subCategoryQuery));
            } else {
                boolQueryBuilder.must(QueryBuilders.regexpQuery("subCategory", subCategoryQuery));
            }
        }
        if (foldersQuery != null) {
            if (!regexp) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("folders", foldersQuery));
            } else {
                boolQueryBuilder.must(QueryBuilders.regexpQuery("folders", foldersQuery));
            }
        }

        if (titleQuery != null) {
            if (!regexp) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("title", titleQuery));
            } else {
                boolQueryBuilder.must(QueryBuilders.regexpQuery("title", titleQuery));
            }
        }

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(page, 20));
        searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC));

        Page<TorrentInfoVO> result = torrentInfoRepository.search(searchQueryBuilder.build());

        int index = 0;
        for (TorrentInfoVO entry : result) {
            System.out.printf("%d. [%s] %s%n", (index+1), entry.getForum(), entry.getTitle());
        }
        System.out.println("total: ${result.totalElements}");
    }

    public TorrentInfoVO findTorrent(String torrentId) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        boolQueryBuilder.must(QueryBuilders.termsQuery("id", torrentId));

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(0, 10));
        searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC));

        Page<TorrentInfoVO> result = torrentInfoRepository.search(searchQueryBuilder.build());

        return result.getTotalElements() > 0
                ? result.getContent().iterator().next()
                : null;
    }

    public Page<TorrentInfoVO> findPage(String[] forumQueries, String[] titleTerms, int page, int pageSize) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (forumQueries != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("forumId", ElasticUtils.toLowerAll(forumQueries)));
        }
        if (titleTerms != null) {
//            boolQueryBuilder.must(QueryBuilders.termsQuery("title", titleQuery));

            final BoolQueryBuilder builder = QueryBuilders.boolQuery();
            for (String term : titleTerms) {
                builder.should(QueryBuilders.wildcardQuery("title", term));
            }
            builder.minimumNumberShouldMatch(titleTerms.length);

            boolQueryBuilder.must(builder);
        }

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(page, pageSize));
        searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC));

        Page<TorrentInfoVO> result = torrentInfoRepository.search(searchQueryBuilder.build());

        return result;
    }

    public Page<TorrentInfoVO> findPage(String[] mainCategoryQuery, String[] subCategoryQuery, String[] foldersQuery, String titleQuery, Integer page) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (mainCategoryQuery != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("mainCategory", ElasticUtils.toLowerAll(mainCategoryQuery)));
        }
        if (subCategoryQuery != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("subCategory", ElasticUtils.toLowerAll(subCategoryQuery)));
        }
        if (foldersQuery != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("folders", ElasticUtils.toLowerAll(foldersQuery)));
        }

        if (titleQuery != null) {
            boolQueryBuilder.must(QueryBuilders.wildcardQuery("title", titleQuery));
//            boolQueryBuilder.must(QueryBuilders.termsQuery("title", titleQuery))
        }

        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withPageable(new PageRequest(page, 20));
        searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC));

        Page<TorrentInfoVO> result = torrentInfoRepository.search(searchQueryBuilder.build());

        return result;
    }

}
