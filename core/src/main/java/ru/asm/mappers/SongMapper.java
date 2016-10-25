package ru.asm.mappers;

import org.apache.ibatis.annotations.*;
import ru.asm.domain.SongVO;

/**
 * User: artem.smirnov
 * Date: 15.06.2016
 * Time: 10:27
 */
public interface SongMapper {

    @Results(id = "songResult", value = {
            @Result(property = "artist", column = "artist"),
            @Result(property = "title", column = "title")
    })
    @Select("SELECT * FROM songs WHERE artist = #{artist} AND title = #{title}")
    SongVO getSong(@Param("artist") String artist, @Param("title") String title);

    @Select("SELECT * FROM songs WHERE playlist = #{playlist}")
    SongVO findSongsByPlaylist(@Param("playlist") String playlist);

    @Select("SELECT * FROM songs WHERE playlist = #{playlist} AND artist = #{artist}")
    SongVO findSongsByPlaylistAndArtist(@Param("playlist") String playlist, @Param("artist") String artist);

    @Insert("insert into songs values (#{artist}, #{title}, #{playlist}, #{comment})")
    void insertSong(@Param("artist") String artist, @Param("title") String title, @Param("playlist") String playlist, @Param("comment") String comment);

    @Delete("delete from songs where playlist = #{playlist}")
    void deleteSongs(@Param("playlist") String playlist);

}
