package ru.asm.tools;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.AppConfiguration;
import ru.asm.core.AppCoreService;

/**
 * User: artem.smirnov
 * Date: 14.03.2017
 * Time: 14:05
 */
class DownloadTorrentsDbTool {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        final AppCoreService appCoreService = applicationContext.getBean(AppCoreService.class);

//        String magnet = "magnet:?xt=urn:btih:77D5646C8645A40BDB635FBED263B5B3A86BCB0F&tr=http%3A%2F%2Fbt2.t-ru.org%2Fann%3Fmagnet&dn=XML%20%D0%B1%D0%B0%D0%B7%D0%B0%20%D1%80%D0%B0%D0%B7%D0%B4%D0%B0%D1%87%20RuTracker.ORG%20v.0.1.20170208";
        String magnet = "magnet:?xt=urn:btih:77D5646C8645A40BDB635FBED263B5B3A86BCB0F";
        final AppConfiguration appConfiguration = AppConfiguration.getInstance();
        appCoreService.downloadTorrent(appConfiguration.getTorrentBackupDownloadFile(), magnet);
    }
}
