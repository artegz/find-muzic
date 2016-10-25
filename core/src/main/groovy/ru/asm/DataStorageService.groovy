package ru.asm

import edu.fm.FileTools
import edu.fm.SongDescriptor
import org.apache.ibatis.session.SqlSessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.asm.domain.SongVO
import ru.asm.mappers.SongMapper

/**
 * User: artem.smirnov
 * Date: 17.06.2016
 * Time: 9:47
 */
@Component
class DataStorageService {

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    public void executeSql(String sql) {
        def connection = sqlSessionFactory.openSession().getConnection()
        def statement = connection.createStatement()
        if (statement.execute(sql)) {
            def rs = statement.getResultSet()

            def metaData = rs.getMetaData()

            int n = 1

            while (rs.next()) {
                print "${n++}. "
                for (int i = 1; i <= metaData.columnCount; i++) {
                    print rs.getString(i)
                    print '\t'
                }
                println ""
            }
        }
    }

    public void cleanupSongs(String playlist) {
        songMapper.deleteSongs(playlist)
    }

    public void listSongs(String artist, String playlist) {
        SongVO songs = findSongs(artist, playlist)

        int i = 1;

        for (SongVO song : songs) {
            println "${i++}. [${song.artist}]"
        }
    }

    public void listArtists(String playlist) {
        SongVO songs = findSongs(null, playlist)

        def artists = new HashSet<String>()

        for (SongVO song : songs) {
            artists.add(song.artist)
        }

        artists.each { a ->
            println a
        }
    }

    public void importPlaylist(File file, String playlist, String comment) {
        def songs = FileTools.readCsv(file)

        int i = 1;
        for (SongDescriptor song : songs) {
            songMapper.insertSong(song.artist, song.title, playlist, comment)
        }

        def dbSongs = songMapper.getSong(null, null)
        for (SongVO song : dbSongs) {
            println "${i++}. [${song.artist}] ${song.title} into ${playlist} with comment: ${comment}"
        }
    }


    private SongVO findSongs(String artist, String playlist) {
        def songs
        if (artist != null && playlist != null) {
            songs = songMapper.findSongsByPlaylistAndArtist(playlist, artist)
        } else {
            songs = songMapper.findSongsByPlaylist(playlist)
        }
        songs
    }
}
