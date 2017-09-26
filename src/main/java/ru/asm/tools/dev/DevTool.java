package ru.asm.tools.dev;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 16:30
 */
public class DevTool {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm.core.persistence");
        final PlaylistSongsMapper playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);

        final List<String> artists = playlistSongsMapper.getAllArtists();

        for (String artist : artists) {
            System.out.println(artist);
        }
    }
}
