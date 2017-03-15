package ru.asm

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.FieldSortBuilder
import org.elasticsearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component
import ru.asm.domain.TorrentInfoVO
import ru.asm.repositories.TorrentInfoRepository

/**
 * User: artem.smirnov
 * Date: 22.06.2016
 * Time: 9:26
 */
@Component
class TorrentsDatabaseService {

    @Autowired
    private TorrentInfoRepository torrentInfoRepository

    TorrentsDatabaseService() {
    }

    TorrentsDatabaseService(TorrentInfoRepository torrentInfoRepository) {
        this.torrentInfoRepository = torrentInfoRepository
    }

    public void printTotalCount() {
        def count = torrentInfoRepository.count()
        println "${count} records"
    }

    public void findAndPrint(boolean regexp, String mainCategoryQuery, String subCategoryQuery, String foldersQuery, String titleQuery, Integer page) {
        def searchQueryBuilder = new NativeSearchQueryBuilder()

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()

        if (mainCategoryQuery != null) {
            if (!regexp) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("mainCategory", mainCategoryQuery))
            } else {
                boolQueryBuilder.must(QueryBuilders.regexpQuery("mainCategory", mainCategoryQuery))
            }
        }
        if (subCategoryQuery != null) {
            if (!regexp) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("subCategory", subCategoryQuery))
            } else {
                boolQueryBuilder.must(QueryBuilders.regexpQuery("subCategory", subCategoryQuery))
            }
        }
        if (foldersQuery != null) {
            if (!regexp) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("folders", foldersQuery))
            } else {
                boolQueryBuilder.must(QueryBuilders.regexpQuery("folders", foldersQuery))
            }
        }

        if (titleQuery != null) {
            if (!regexp) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("title", titleQuery))
            } else {
                boolQueryBuilder.must(QueryBuilders.regexpQuery("title", titleQuery))
            }
        }

        searchQueryBuilder.withQuery(boolQueryBuilder)
        searchQueryBuilder.withPageable(new PageRequest(page, 20))
        searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC))

        def result = torrentInfoRepository.search(searchQueryBuilder.build())

        result.eachWithIndex { TorrentInfoVO entry, int index ->
            println "${index + 1}. [${entry.getMainCategory()} | ${entry.getSubCategory()} | ${entry.getFolders()}] ${entry.getTitle()}"
        }
        println("total: ${result.totalElements}")
    }

    public Page<TorrentInfoVO> findPage(String[] mainCategoryQuery, String[] subCategoryQuery, String[] foldersQuery, String titleQuery, Integer page) {
        def searchQueryBuilder = new NativeSearchQueryBuilder()

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()

        if (mainCategoryQuery != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("mainCategory", toLowerAll(mainCategoryQuery)))
        }
        if (subCategoryQuery != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("subCategory", toLowerAll(subCategoryQuery)))
        }
        if (foldersQuery != null) {
            boolQueryBuilder.must(QueryBuilders.termsQuery("folders", toLowerAll(foldersQuery)))
        }

        if (titleQuery != null) {
            boolQueryBuilder.must(QueryBuilders.wildcardQuery("title", titleQuery))
//            boolQueryBuilder.must(QueryBuilders.termsQuery("title", titleQuery))
        }

        searchQueryBuilder.withQuery(boolQueryBuilder)
        searchQueryBuilder.withPageable(new PageRequest(page, 20))
        searchQueryBuilder.withSort(new FieldSortBuilder("title").order(SortOrder.ASC))

        Page<TorrentInfoVO> result = torrentInfoRepository.search(searchQueryBuilder.build())

        result
    }

    private ArrayList<String> toLowerAll(String[] origVals) {
        def result = new ArrayList<String>()
        for (String origVal : origVals) {
            result.add(origVal.toLowerCase())
//            result.add(origVal)
        }
        result
    }

}