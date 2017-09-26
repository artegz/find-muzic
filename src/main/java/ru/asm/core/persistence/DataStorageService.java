package ru.asm.core.persistence;

import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;

import java.sql.*;

/**
 * User: artem.smirnov
 * Date: 17.06.2016
 * Time: 9:47
 */
@Component
public class DataStorageService {

    private static final Logger logger = LoggerFactory.getLogger(DataStorageService.class);

    @Autowired
    private PlaylistSongsMapper playlistSongsMapper;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    public void executeSql(String sql) {
        try {
            Connection connection = sqlSessionFactory.openSession().getConnection();
            Statement statement = connection.createStatement();
            if (statement.execute(sql)) {
                ResultSet rs = statement.getResultSet();

                ResultSetMetaData metaData = rs.getMetaData();

                StringBuilder sb;
                sb = new StringBuilder();

                final int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    sb.append(metaData.getColumnName(i))
                            .append("\t|\t");
                }
                sb.append(System.lineSeparator());

                int n = 1;
                while (rs.next()) {
                    sb.append(String.valueOf(n++))
                            .append(". ");
                    for (int i = 1; i <= columnCount; i++) {
                        sb.append(rs.getString(i))
                        .append("\t|\t");
                    }
                    sb.append(System.lineSeparator());
                }

                System.out.println(sb.toString());
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

//    public void cleanupSongs(String playlist) {
//        songMapper.deleteSongs(playlist)
//    }
//
//    public void listSongs(String artist, String playlist) {
//        SongVO songs = resolveSongs(artist, playlist)
//
//        int i = 1;
//
//        for (SongVO song : songs) {
//            println "${i++}. [${song.artist}]"
//        }
//    }
//
//    public void listArtists(String playlist) {
//        SongVO songs = resolveSongs(null, playlist)
//
//        def artists = new HashSet<String>()
//
//        for (SongVO song : songs) {
//            artists.add(song.artist)
//        }
//
//        artists.each { a ->
//            println a
//        }
//    }
//
//    public void importPlaylist(File file, String playlist, String comment) {
//        def songs = FileTools.readCsv(file)
//
//        int i = 1;
//        for (SongDescriptor song : songs) {
//            songMapper.insertSong(song.artist, song.title, playlist, comment)
//        }
//
//        def dbSongs = songMapper.getSong(null, null)
//        for (SongVO song : dbSongs) {
//            println "${i++}. [${song.artist}] ${song.title} into ${playlist} with comment: ${comment}"
//        }
//    }
//
//
//    private SongVO resolveSongs(String artist, String playlist) {
//        def songs
//        if (artist != null && playlist != null) {
//            songs = songMapper.findSongsByPlaylistAndArtist(playlist, artist)
//        } else {
//            songs = songMapper.findSongsByPlaylist(playlist)
//        }
//        songs
//    }
}
