package ru.asm.core.persistence.mappers;

import org.apache.ibatis.annotations.*;
import ru.asm.core.persistence.domain.PlaylistSongEntity;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 15.06.2016
 * Time: 10:27
 */
public interface PlaylistSongsMapper {

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
    List<PlaylistSongEntity> getSongsByPlaylist(@Param("playlist") String playlist);

    @Results(id = "songResult3", value = {
            @Result(property = "artist", column = "artist"),
            @Result(property = "artistId", column = "artist_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "songId", column = "song_id"),
    })
    @Select("SELECT t2.artist, t2.artist_id, t3.TITLE, t3.song_id " +
            "FROM ARTISTS t2 " +
            "join SONGS t3 on t3.artist_id = t2.artist_id " +
            "where t3.song_id = #{songId} ")
    PlaylistSongEntity getSongById(@Param("songId") Integer songId);

    @Select("SELECT artist FROM ARTISTS where artist_id = #{artistId}")
    String getArtistNameById(@Param("artistId") Integer artistId);

    @Select("SELECT artist_id FROM ARTISTS where artist = #{artist, jdbcType=VARCHAR}")
    Integer findArtistIdByName(@Param("artist") String artist);

    @Select("SELECT song_id FROM SONGS where artist_id = #{artistId} and title = #{title, jdbcType=VARCHAR}")
    Integer findSongIdByNameAndArtist(@Param("artistId") Integer artistId, @Param("title") String title);



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


}
