package ru.asm.core.index;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import ru.asm.core.index.repositories.TorrentInfoRepository;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 16:31
 */
public class RepositoryTemplate {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryTemplate.class);

    org.elasticsearch.node.Node node;
    TorrentInfoRepository torrentInfoRepository;
    ElasticsearchOperations elasticsearchOperations;

    public RepositoryTemplate(ApplicationContext applicationContext) {
        this.node = applicationContext.getBean(org.elasticsearch.node.Node.class);
        this.torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class);
        this.elasticsearchOperations = applicationContext.getBean(ElasticsearchOperations.class);
    }

    public void withRepository(RepositoryAction r) {
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
                throw new IllegalArgumentException("repository is empty");
            } else {
                logger.info("{} entries in repository", count);
            }

            r.doAction(torrentInfoRepository);
        } finally {
            node.close();
        }
    }

}
