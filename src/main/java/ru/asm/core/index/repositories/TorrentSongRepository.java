package ru.asm.core.index.repositories;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import ru.asm.core.index.domain.TorrentSongVO;

/**
 * User: artem.smirnov
 * Date: 07.06.2016
 * Time: 10:01
 */
public interface TorrentSongRepository extends ElasticsearchRepository<TorrentSongVO, String> {
}
