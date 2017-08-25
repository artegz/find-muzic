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

    public static void parseDocument(InputStream inputStream, int groupSize, GroupHandler groupHandler) throws XMLStreamException {
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
}
