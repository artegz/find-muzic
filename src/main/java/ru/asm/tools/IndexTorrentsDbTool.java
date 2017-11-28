package ru.asm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.AppCoreServiceImpl;

import java.io.*;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 9:10
 */
class IndexTorrentsDbTool {

    private static final Logger logger = LoggerFactory.getLogger(IndexTorrentsDbTool.class);

    public static void main(String[] args) {
        final File backup = new File("C:\\TEMP\\find-music\\rutracker_org_db\\backup.20170208185701\\backup.20170208185701.xml");

        logInfo("initializing application context");
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        logInfo("application context initialized");

        final AppCoreServiceImpl appCoreService = applicationContext.getBean(AppCoreServiceImpl.class);
        appCoreService.indexTorrentsDb(backup);
    }

    private static void logInfo(String message, Object... args) {
        logger.info(String.format(message, args));
    }

}
