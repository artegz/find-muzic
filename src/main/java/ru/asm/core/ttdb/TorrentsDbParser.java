package ru.asm.core.ttdb;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.asm.core.index.domain.TorrentInfoVO;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 05.04.2017
 * Time: 9:41
 */
public class TorrentsDbParser {

    public static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy.MM.dd HH:mm:ss").withZoneUTC();
    public static final String TORRENT = "torrent";
    public static final String FORUM = "forum";
    public static final String TORRENT1 = "torrent";
    public static final String TITLE = "title";
    public static final String MAGNET = "magnet";
    public static final String FORUM1 = "forum";
    public static final String CONTENT = "content";

    public static void parseDocument(InputStream inputStream, int groupSize, GroupHandler groupHandler) throws XMLStreamException {
        List<TorrentInfoVO> torrentInfosGroup = new ArrayList<>();
        TorrentInfoVO currentTorrentInfo = null;

        StringBuilder tagContent = null;
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLStreamReader reader = factory.createXMLStreamReader(inputStream, "UTF-8");

        while(reader.hasNext()){
            Integer event = null;
            try {
                event = reader.next();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
            if (event == null) {
                throw new AssertionError();
            }

            switch(event){
                case XMLStreamConstants.START_ELEMENT:
                    String tagName = reader.getLocalName();
                    switch (tagName) {
                        case TORRENT: {
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
                            break;
                        }
                        case FORUM: {
                            final Map<String, String> attributes = getAttributes(reader);
                            if (attributes.containsKey("id")) {
                                assert (currentTorrentInfo != null);
                                currentTorrentInfo.setForumId(attributes.get("id"));
                            }
                            tagContent = new StringBuilder();
                            break;
                        }
                        case TITLE:
                        case MAGNET:
                        case CONTENT:
                            tagContent = new StringBuilder();
                            break;
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                case XMLStreamConstants.CDATA:
                    if (tagContent != null) {
                        tagContent.append(reader.getText().trim());
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    switch(reader.getLocalName()){
                        case TORRENT:
                            torrentInfosGroup.add(currentTorrentInfo);
                            break;
                        case TITLE:
                            assert (currentTorrentInfo != null);
                            assert (tagContent != null);
                            currentTorrentInfo.setTitle(tagContent.toString());
                            tagContent = null;
                            break;
                        case MAGNET:
                            assert (currentTorrentInfo != null);
                            assert (tagContent != null);
                            currentTorrentInfo.setMagnet(tagContent.toString());
                            tagContent = null;
                            break;
                        case FORUM:
                            assert (currentTorrentInfo != null);
                            assert (tagContent != null);
                            currentTorrentInfo.setForum(tagContent.toString());
                            tagContent = null;
                            break;
                        case CONTENT:
                            // ignore
                            tagContent = null;
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
}
