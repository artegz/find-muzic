package ru.asm.tools;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.AppCoreService;

/**
 * User: artem.smirnov
 * Date: 17.03.2017
 * Time: 17:14
 */
class IndexArtistMp3Tool {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        final AppCoreService appCoreService = applicationContext.getBean(AppCoreService.class);
        appCoreService.resolveSongs_mp3(null, null);
    }
}
