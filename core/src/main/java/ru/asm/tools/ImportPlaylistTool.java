package ru.asm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.FileTools;
import ru.asm.core.SongDescriptor;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;

import java.io.File;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 16:47
 */
class ImportPlaylistTool {

    private static final Logger logger = LoggerFactory.getLogger(ImportPlaylistTool.class);

    public static void main(String[] args) {
        String playlist = "nashe";
        String comment = "test";
        final File file = new File("C:\\IdeaProjects\\find-muzic\\core\\src\\main\\resources\\playlists/102015.nashe.playlist.txt");

        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");
        final PlaylistSongsMapper playlistSongsMapper = applicationContext.getBean(PlaylistSongsMapper.class);

        List<SongDescriptor> songs = FileTools.readCsv(file);

        int i = 1;
        for (SongDescriptor song : songs) {
            final String artist = song.getArtist();
            final String songTitle = song.getTitle();

            if (artist != null && !artist.isEmpty() && songTitle != null && !artist.isEmpty()) {
                Integer artistId = playlistSongsMapper.findArtistId(artist);
                if (artistId == null) {
                    playlistSongsMapper.insertArtist(null, artist);
                    artistId = playlistSongsMapper.findArtistId(artist);
                }
                assert (artistId != null);


                Integer songId = playlistSongsMapper.findSongId(artistId, songTitle);
                if (songId == null) {
                    playlistSongsMapper.insertSong(null, artistId, songTitle);
                    songId = playlistSongsMapper.findSongId(artistId, songTitle);
                }
                assert (songId != null);


                playlistSongsMapper.insertPlaylistSong(artistId, songId, playlist, comment);
                logger.info("{}. [{}] '{}' into '{}' with comment: '{}'", i++, song.getArtist(), song.getTitle(), playlist, comment);
            } else {
                logger.warn("{}. null", i++);
            }
        }
    }
}
