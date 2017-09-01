package ru.asm.core.persistence.mappers;

import org.apache.ibatis.annotations.*;
import ru.asm.core.persistence.domain.PlaylistSongEntity;
import ru.asm.core.persistence.domain.ResolvedSongEntity;
import ru.asm.core.persistence.domain.StatusEntity;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 15.06.2016
 * Time: 10:27
 */
public interface PlaylistSongsMapper {


    @Results(id = "statusEntitiesResult", value = {
            @Result(property = "artistId", column = "artist_id"),
            @Result(property = "artist", column = "artist"),
            @Result(property = "torrentId", column = "torrent_id"),
            @Result(property = "format", column = "format"),
            @Result(property = "status", column = "status"),
    })
    @Select("SELECT A.ARTIST_ID, A.ARTIST, T.TORRENT_ID, T.FORMAT, T.STATUS\n" +
            "FROM ARTISTS A \n" +
            "LEFT JOIN ARTISTS_TORRENTS AT ON A.ARTIST_ID = AT.ARTIST_ID \n" +
            "LEFT JOIN TORRENTS T ON T.TORRENT_ID = AT.TORRENT_ID")
    List<StatusEntity> getStatuses();

    @Results(id = "songResult2", value = {
            @Result(property = "artist", column = "artist"),
            @Result(property = "artistId", column = "artist_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "songId", column = "song_id"),
    })
    @Select("SELECT t2.artist, t2.artist_id, t3.TITLE, t3.song_id " +
            "FROM PLAYLIST_SONGS t1 " +
            "join ARTISTS t2 on t1.artist_id = t2.artist_id " +
            "join SONGS t3 on t3.song_id = t1.song_id " +
            "where t1.playlist = #{playlist} " +
            "order by t1.order_id")
    List<PlaylistSongEntity> getSongs(@Param("playlist") String playlist);

    @Select("SELECT distinct artist FROM ARTISTS")
    List<String> getAllArtists();

    @Select("SELECT distinct artist_id FROM ARTISTS")
    List<Integer> getAllArtistsIds();

    @Select("SELECT artist FROM ARTISTS where artist_id = #{artistId}")
    String getArtist(@Param("artistId") Integer artistId);

    @Select("SELECT artist_id FROM ARTISTS where artist = #{artist, jdbcType=VARCHAR}")
    Integer findArtistId(@Param("artist") String artist);

    @Select("SELECT song_id FROM SONGS where artist_id = #{artistId} and title = #{title, jdbcType=VARCHAR}")
    Integer findSongId(@Param("artistId") Integer artistId, @Param("title") String title);

//
//    @Select("SELECT * FROM PLAYLIST_SONGS WHERE playlist = #{playlist}")
//    SongEntity findSongsByPlaylist(@Param("playlist") String playlist);
//
//    @Select("SELECT * FROM PLAYLIST_SONGS WHERE playlist = #{playlist} AND artist = #{artist}")
//    SongEntity findSongsByPlaylistAndArtist(@Param("playlist") String playlist, @Param("artist") String artist);



    @Insert("insert into PLAYLIST_SONGS values (#{artistId}, #{songId}, #{playlist}, #{comment}, #{orderId})")
    void insertPlaylistSong(@Param("artistId") Integer artistId,
                            @Param("songId") Integer songId,
                            @Param("playlist") String playlist,
                            @Param("comment") String comment,
                            @Param("orderId") Integer orderId);

    @Insert("insert into ARTISTS (artist_id, artist) values (#{artistId, jdbcType=INTEGER}, #{artist})")
    @SelectKey(statement="select nvl(max(artist_id), 0) + 1 FROM ARTISTS", keyProperty="artistId", before=true, resultType=Integer.class)
    void insertArtist(@Param("artistId") Integer artistId, @Param("artist") String artist);

    @Insert("insert into SONGS values (#{songId}, #{artistId}, #{title})")
    @SelectKey(statement="select nvl(max(song_id), 0) + 1 FROM SONGS", keyProperty="songId", before=true, resultType=int.class)
    void insertSong(@Param("songId") Integer songId, @Param("artistId") Integer artistId, @Param("title") String title);


    @Insert("insert into TORRENTS values (#{torrentId}, #{format}, #{forumId}, #{status})")
    void insertTorrent(@Param("torrentId") String torrentId,
                       @Param("format") String format,
                       @Param("forumId") String forumId,
                       @Param("status") String status);

    @Insert("insert into ARTISTS_TORRENTS values (#{artistId},#{torrentId})")
    void insertArtistTorrentLink(@Param("artistId") Integer artistId,
                                 @Param("torrentId") String torrentId);

    @Update("UPDATE TORRENTS SET STATUS = #{status} WHERE TORRENT_ID = #{torrentId}")
    void updateTorrentStatus(@Param("torrentId") String torrentId,
                             @Param("status") String status);

    @Select("SELECT t1.TORRENT_ID FROM ARTISTS_TORRENTS t1 join TORRENTS t2 on t1.TORRENT_ID = t2.TORRENT_ID WHERE t1.ARTIST_ID = #{artistId} AND t2.FORMAT = #{format} AND t2.STATUS = #{status}")
    List<String> getArtistTorrents2(@Param("artistId") Integer artistId,
                                   @Param("format") String format,
                                   @Param("status") String status);


    @Insert("insert into SONGS_TORRENTS values (#{songId},#{artistId},#{torrentId},#{fileId})")
    void insertSongTorrentLink(@Param("songId") Integer songId,
                               @Param("artistId") Integer artistId,
                               @Param("torrentId") String torrentId,
                               @Param("fileId") String fileId);

    @Results(id = "songResult", value = {
            @Result(property = "artist", column = "artist"),
            @Result(property = "artistId", column = "artist_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "songId", column = "song_id"),
            @Result(property = "torrentId", column = "torrent_id"),
            @Result(property = "fileId", column = "file_id"),
    })
    @Select("select a.artist_id, a.artist, s.song_id, s.title, st.torrent_id, st.file_id " +
            "from songs s " +
            "join artists a on a.artist_id = s.artist_id " +
            "join SONGS_TORRENTS st on st.song_id = s.song_id")
    List<ResolvedSongEntity> getFoundSongs();

//    @Insert("insert into ARTISTS_TORRENTS values (#{artistId}, }#{torrentId})")
//    void insertArtistTorrentLink(@Param("artistId") Integer artistId,
//                                 @Param("torrentId") String torrentId);




    @Deprecated
    @Delete("delete from ARTIST_TORRENTS_STATUS WHERE format = #{format}")
    void deleteAllArtistTorrent(@Param("format") String format);

    @Deprecated
    @Insert("insert into ARTIST_TORRENTS_STATUS values (#{artistId}, #{torrentId}, #{format}, #{forumId}, #{status})")
    void insertArtistTorrent(@Param("artistId") Integer artistId,
                             @Param("torrentId") String torrentId,
                             @Param("format") String format,
                             @Param("forumId") String forumId,
                             @Param("status") String status);

    @Deprecated
    @Select("SELECT TORRENT_ID FROM ARTIST_TORRENTS_STATUS WHERE ARTIST_ID = #{artistId} AND FORMAT = #{format} AND STATUS = #{status}")
    List<String> getArtistTorrents(@Param("artistId") Integer artistId,
                                   @Param("format") String format,
                                   @Param("status") String status);

    @Deprecated
    @Update("UPDATE ARTIST_TORRENTS_STATUS SET STATUS = #{status} WHERE TORRENT_ID = #{torrentId} AND ARTIST_ID = #{artistId} AND FORMAT = #{format}")
    void updateArtistTorrentStatus(@Param("artistId") Integer artistId,
                                   @Param("format") String format,
                                   @Param("torrentId") String torrentId,
                                   @Param("status") String status);



    @Insert("insert into SONGS_STORAGE values (#{songId}, #{downloadPath})")
    void insertDownloadedSong(@Param("songId") Integer songId,
                              @Param("downloadPath") String downloadPath);

    @Select("SELECT A.ARTIST||': '||S.TITLE FROM SONGS S JOIN ARTISTS A ON A.ARTIST_ID = S.ARTIST_ID JOIN SONGS_STORAGE SS ON S.SONG_ID = S.SONG_ID")
    List<String> getDownloadedSongsNames();

}
