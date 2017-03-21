package ru.asm.tools;

import org.apache.commons.lang.mutable.MutableLong;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.node.Node;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.format.number.NumberStyleFormatter;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 9:10
 */
class TorrentsDbIndexTool {

    private static final Logger logger = LoggerFactory.getLogger(TorrentsDbIndexTool.class);

    public static final NumberFormat format = new NumberStyleFormatter().getNumberFormat(Locale.ENGLISH);
    public static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy.MM.dd HH:mm:ss").withZoneUTC();

    public static final int GROUP_SIZE = 10000;

    public static void main(String[] args) {
        final File backup = new File("C:\\TEMP\\find-music\\rutracker_org_db\\backup.20170208185701\\backup.20170208185701.xml");
        final InputStream inputStream;
        try {
            inputStream = new FileInputStream(backup);
        } catch (FileNotFoundException e) {
            logInfo(e.getMessage());
            e.printStackTrace();
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
                parseDocument(inputStream, GROUP_SIZE, new GroupHandler() {

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

    static void parseDocument(InputStream inputStream, int groupSize, GroupHandler groupHandler) throws XMLStreamException {
        List<TorrentInfoVO> torrentInfosGroup = new ArrayList<>();
        TorrentInfoVO currentTorrentInfo = null;

        String tagContent = null;
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

        while(reader.hasNext()){
            int event = reader.next();

            switch(event){
                case XMLStreamConstants.START_ELEMENT:
                    if ("torrent".equals(reader.getLocalName())){
                        currentTorrentInfo = new TorrentInfoVO();

                        final Map<String, String> attributes = getAttributes(reader);

                        if (attributes.containsKey("registred_at")) {
                            final DateTime registeredAt = formatter.parseDateTime(attributes.get("registred_at"));
                            currentTorrentInfo.setCreationDate(registeredAt.toDate());
                        }

                        if (attributes.containsKey("size")) {
                            final Long size = Long.valueOf(attributes.get("size"));
                            currentTorrentInfo.setSize(size);
                        }

                        if (attributes.containsKey("id")) {
                            final String id = attributes.get("id");
                            currentTorrentInfo.setId(id);
                        }
                    } else if ("forum".equals(reader.getLocalName())) {
                        final Map<String, String> attributes = getAttributes(reader);
                        if (attributes.containsKey("id")) {
                            assert (currentTorrentInfo != null);
                            currentTorrentInfo.setForumId(attributes.get("id"));
                        }
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    tagContent = reader.getText().trim();
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    switch(reader.getLocalName()){
                        case "torrent":
                            torrentInfosGroup.add(currentTorrentInfo);
                            break;
                        case "title":
                            assert (currentTorrentInfo != null);
                            currentTorrentInfo.setTitle(tagContent);
                            break;
                        case "magnet":
                            assert (currentTorrentInfo != null);
                            currentTorrentInfo.setMagnet(tagContent);
                            break;
                        case "forum":
                            assert (currentTorrentInfo != null);
                            currentTorrentInfo.setForum(tagContent);
                            break;
                        case "content":
                            // ignore
                            break;
                    }
                    break;

                case XMLStreamConstants.START_DOCUMENT:
                    break;
            }

            if (torrentInfosGroup.size() >= groupSize) {
                groupHandler.handleGroup(torrentInfosGroup);
                torrentInfosGroup = new ArrayList<>();
            }
        }

        if (!torrentInfosGroup.isEmpty()) {
            groupHandler.handleGroup(torrentInfosGroup);
        }
    }

    private static Map<String, String> getAttributes(XMLStreamReader reader) {
        final Map<String, String> attributes = new HashMap<>();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
        }
        return attributes;
    }

    private static void logInfo(String message, Object... args) {
        logger.info(String.format(message, args));
    }

    interface GroupHandler {

        void handleGroup(List<TorrentInfoVO> torrentInfos);
    }
}
