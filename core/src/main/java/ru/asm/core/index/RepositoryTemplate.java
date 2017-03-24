package ru.asm.core.index;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.io.Serializable;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 16:31
 */
public class RepositoryTemplate<K, V extends Serializable> {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryTemplate.class);

    org.elasticsearch.node.Node node;
    ElasticsearchRepository<K, V> torrentInfoRepository;
    ElasticsearchOperations elasticsearchOperations;

    public RepositoryTemplate(ApplicationContext applicationContext, ElasticsearchRepository<K, V> repository) {
        this.node = applicationContext.getBean(org.elasticsearch.node.Node.class);
        this.torrentInfoRepository = repository;
        this.elasticsearchOperations = applicationContext.getBean(ElasticsearchOperations.class);
    }

    public void withRepository(RepositoryAction<K, V> r) {
        try {
            // await at least for yellow status
            final ClusterHealthResponse response = elasticsearchOperations.getClient()
                    .admin()
                    .cluster()
                    .prepareHealth()
                    .setWaitForGreenStatus()
                    .get();
            if (response.getStatus() != ClusterHealthStatus.YELLOW) {
                throw new IllegalStateException("repository is not initialized");
            }

            Long count = torrentInfoRepository.count();
            if (count <= 0) {
                logger.info("repository is empty");
            } else {
                logger.info("{} entries in repository", count);
            }

            r.doAction(torrentInfoRepository);
        } finally {
            node.close();
        }
    }

}
