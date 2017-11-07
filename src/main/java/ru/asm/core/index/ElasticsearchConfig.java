package ru.asm.core.index;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import ru.asm.core.AppConfiguration;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * User: artem.smirnov
 * Date: 17.06.2016
 * Time: 9:28
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = {"org/springframework/data/elasticsearch/repositories", "ru/asm/core/index/repositories"})
public class ElasticsearchConfig {

    @Bean
    public Node node() {
        final Node node = nodeBuilder()
                .local(true)
                .settings(
                        Settings.builder()
                                .put("path.home", AppConfiguration.ES_HOME_LOCATION)
                                .put("http.enabled", "false")
                                .put("path.data", AppConfiguration.ES_DATA_LOCATION)
                                .build()
                )
                .node();
        return node;
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate(Node node) {
        final Client client = node.client();
        return new ElasticsearchTemplate(client);
    }

}
