package ru.asm.tools;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.AppCoreService;

/**
 * User: artem.smirnov
 * Date: 31.03.2017
 * Time: 12:06
 */
public class ResolveArtistTorrentsTool {

    public static void main(String[] args) {
        // init
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        final AppCoreService appCoreService = applicationContext.getBean(AppCoreService.class);
        appCoreService.resolveArtists();
    }
}


