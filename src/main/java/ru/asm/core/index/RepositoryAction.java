package ru.asm.core.index;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.io.Serializable;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 16:31
 */
public interface RepositoryAction<K, V extends Serializable> {

    void doAction(ElasticsearchRepository<K, V> repo);
}
