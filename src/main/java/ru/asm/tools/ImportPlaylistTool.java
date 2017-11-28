package ru.asm.tools;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.AppCoreServiceImpl;

import java.io.File;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 16:47
 */
class ImportPlaylistTool {

    public static void main(String[] args) {
        final String playlist = "nashe";
        final String comment = "test";
        final File file = new File("C:\\IdeaProjects\\find-muzic\\core\\src\\main\\resources\\playlists/102015.nashe.playlist.txt");

        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        final AppCoreServiceImpl appCoreService = applicationContext.getBean(AppCoreServiceImpl.class);

        appCoreService.importPlaylist(playlist, comment, file);
    }
}
