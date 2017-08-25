package ru.asm.tools;

import org.apache.commons.lang.mutable.MutableLong;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.format.number.NumberStyleFormatter;
import ru.asm.core.ttdb.GroupHandler;
import ru.asm.core.ttdb.TorrentsDbParser;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 9:10
 */
class IndexTorrentsDbTool {

    private static final Logger logger = LoggerFactory.getLogger(IndexTorrentsDbTool.class);

    public static final NumberFormat format = new NumberStyleFormatter().getNumberFormat(Locale.ENGLISH);

    public static final int GROUP_SIZE = 10000;

    public static void main(String[] args) {
        final File backup = new File("C:\\TEMP\\find-music\\rutracker_org_db\\backup.20170208185701\\backup.20170208185701.xml");
        final InputStream inputStream;
        try {
            inputStream = new FileInputStream(backup);
        } catch (FileNotFoundException e) {
            logInfo(e.getMessage());
            return;
        }

        logInfo("initializing application context");
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        Node node = applicationContext.getBean(Node.class);
        TorrentInfoRepository torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class);
        ElasticsearchOperations elasticsearchOperations = applicationContext.getBean(ElasticsearchOperations.class);
        logInfo("application context initialized");

        try {
            // await at least for yellow status
            logInfo("initializing elasticsearch");
            final ClusterHealthResponse response = elasticsearchOperations.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().get();
            if (response.getStatus() != ClusterHealthStatus.YELLOW) {
                throw new AssertionError();
            }
            logInfo("elasticsearch initialized");

            MutableLong totalIndexed = new MutableLong(0);
            Long count = torrentInfoRepository.count();
            logInfo("%d entries in repository", count);

            try {
                logInfo("start reading backup", count);
                TorrentsDbParser.parseDocument(inputStream, GROUP_SIZE, new GroupHandler() {

                    @Override
                    public void handleGroup(List<TorrentInfoVO> torrentInfos) {
                        logInfo("%s more entries read", torrentInfos.size());
                        torrentInfoRepository.save(torrentInfos);
                        totalIndexed.add(torrentInfos.size());
                        logInfo("%d entries added into index (total: %d)", torrentInfos.size(), totalIndexed.longValue());
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
                logInfo(e.getMessage());
            }

            count = torrentInfoRepository.count();
            logInfo("%d entries in repository", count);
        } finally {
            logInfo("closing opened resources");
            try {
                inputStream.close();
            } catch (IOException e) {
                logInfo(e.getMessage());
                e.printStackTrace();
            }
            node.close();
        }
    }

    private static void logInfo(String message, Object... args) {
        logger.info(String.format(message, args));
    }

}
