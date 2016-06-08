package ru.asm;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * User: artem.smirnov
 * Date: 06.06.2016
 * Time: 17:27
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = {
        "org/springframework/data/elasticsearch/repositories",
        "ru/asm/repositories"
})
public class ElasticsearchConfig {

    @Bean
    public Node node() {
        final Node node = nodeBuilder()
                .local(true)
                .settings(
                        Settings.builder()
                                .put("path.home", "C:\\TEMP\\find-music\\es_home")
                                .put("http.enabled", "false")
                                .put("path.data", "C:\\TEMP\\find-music\\es_data")
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
