package ru.asm.tools.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.asm.core.ttdb.GroupHandler;
import ru.asm.core.ttdb.TorrentsDbParser;
import ru.asm.core.index.domain.TorrentInfoVO;

import java.io.*;
import java.util.*;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 9:10
 */
class TorrentsDbPrintCategoriesTool {

    private static final Logger logger = LoggerFactory.getLogger(TorrentsDbPrintCategoriesTool.class);

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

        final Map<String, String> categories = new HashMap<>();

        try {
            try {
                logInfo("start reading backup");
                TorrentsDbParser.parseDocument(inputStream, GROUP_SIZE, new GroupHandler() {

                    @Override
                    public void handleGroup(List<TorrentInfoVO> torrentInfos) {
                        logInfo("%s more entries read", torrentInfos.size());
                        for (TorrentInfoVO torrentInfo : torrentInfos) {
                            categories.put(torrentInfo.getForumId(), torrentInfo.getForum());
                        }
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
                logInfo(e.getMessage());
            }
        } finally {
            logInfo("closing opened resources");
            try {
                inputStream.close();
            } catch (IOException e) {
                logInfo(e.getMessage());
                e.printStackTrace();
            }
        }

        final List<String> sortedCategories = new ArrayList<>(categories.keySet());
        Collections.singletonList(sortedCategories);

        logger.info("CATEGORIES: ");
        for (String sortedCategory : sortedCategories) {
            logger.info("{}: {}", sortedCategory, categories.get(sortedCategory));
        }
    }

    private static void logInfo(String message, Object... args) {
        logger.info(String.format(message, args));
    }

}
