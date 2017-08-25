package ru.asm.core.index;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.node.Node;
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

    private ApplicationContext applicationContext;
    private ElasticsearchRepository<K, V> torrentInfoRepository;

    public RepositoryTemplate(ApplicationContext applicationContext, ElasticsearchRepository<K, V> repository) {
        this.applicationContext = applicationContext;
        this.torrentInfoRepository = repository;
    }

    public void withRepository(RepositoryAction<K, V> r) {
        try {
            initializeRepos(applicationContext);
            r.doAction(torrentInfoRepository);
        } finally {
            destroyRepos(applicationContext);
        }
    }

    public static void destroyRepos(ApplicationContext applicationContext) {
        final Node node = applicationContext.getBean(Node.class);
        node.close();
    }

    public static void initializeRepos(ApplicationContext applicationContext) {
        // await at least for yellow status
        final ElasticsearchOperations elasticsearchOperations = applicationContext.getBean(ElasticsearchOperations.class);
        final ClusterHealthResponse response = elasticsearchOperations.getClient()
                .admin()
                .cluster()
                .prepareHealth()
                .setWaitForGreenStatus()
                .get();
        if (response.getStatus() != ClusterHealthStatus.YELLOW) {
            throw new IllegalStateException("repository is not initialized");
        }
    }

}
