package ru.asm.tools.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.AppCoreService;
import ru.asm.core.SongsSearchResult;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class FindSongsTool {

    private static final Logger logger = LoggerFactory.getLogger(FindSongsTool.class);

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        final AppCoreService appCoreService = applicationContext.getBean(AppCoreService.class);

        final SongsSearchResult songsSearchResult = appCoreService.resolveSongs();

        logger.info("FOUND ({}): {}", songsSearchResult.getFound().size(), songsSearchResult.getFound());
        logger.warn("NOT FOUND ({}): {}", songsSearchResult.getNotFound().size(), songsSearchResult.getNotFound());
    }
}
