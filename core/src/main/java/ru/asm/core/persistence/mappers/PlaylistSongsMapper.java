package ru.asm.core.persistence.mappers;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 15.06.2016
 * Time: 10:27
 */
public interface PlaylistSongsMapper {

//    @Results(id = "songResult", value = {
//            @Result(property = "artist", column = "artist"),
//            @Result(property = "title", column = "title")
//    })
//    @Select("SELECT * FROM PLAYLIST_SONGS WHERE artist = #{artist} AND title = #{title}")
//    SongEntity getSong(@Param("artist") String artist, @Param("title") String title);

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



    @Insert("insert into PLAYLIST_SONGS values (#{artistId}, #{songId}, #{playlist}, #{comment})")
    void insertPlaylistSong(@Param("artistId") Integer artistId, @Param("songId") Integer songId, @Param("playlist") String playlist, @Param("comment") String comment);

    @Insert("insert into ARTISTS (artist_id, artist) values (#{artistId, jdbcType=INTEGER}, #{artist})")
    @SelectKey(statement="select nvl(max(artist_id), 0) + 1 FROM ARTISTS", keyProperty="artistId", before=true, resultType=Integer.class)
    void insertArtist(@Param("artistId") Integer artistId, @Param("artist") String artist);

    @Insert("insert into SONGS values (#{songId}, #{artistId}, #{title})")
    @SelectKey(statement="select nvl(max(song_id), 0) + 1 FROM SONGS", keyProperty="songId", before=true, resultType=int.class)
    void insertSong(@Param("songId") Integer songId, @Param("artistId") Integer artistId, @Param("title") String title);


    @Delete("delete from ARTIST_TORRENTS")
    void deleteAllArtistTorrent();

    @Insert("insert into ARTIST_TORRENTS values (#{artistId}, #{torrentId})")
    void insertArtistTorrent(@Param("artistId") Integer artistId, @Param("torrentId") String torrentId);

}
