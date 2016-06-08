package ru.asm.repositories;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import ru.asm.domain.TorrentInfoVO;

/**
 * User: artem.smirnov
 * Date: 07.06.2016
 * Time: 10:01
 */
public interface TorrentInfoRepository extends ElasticsearchRepository<TorrentInfoVO, String> {
}
