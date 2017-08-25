package ru.asm.core.persistence.mappers;

import org.apache.ibatis.annotations.*;
import ru.asm.core.persistence.domain.PlaylistSongEntity;
import ru.asm.core.persistence.domain.StatusEntity;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 15.06.2016
 * Time: 10:27
 */
public interface PlaylistSongsMapper {


    @Results(id = "statusEntitiesResult", value = {
            @Result(property = "artistId", column = "artistId"),
            @Result(property = "artist", column = "artist"),
            @Result(property = "torrentId", column = "torrentId"),
            @Result(property = "format", column = "format"),
            @Result(property = "status", column = "status"),
    })
    @Select("SELECT A.ARTIST_ID, A.ARTIST, ATS.TORRENT_ID, ATS.FORMAT, ATS.STATUS\n" +
            "FROM ARTISTS A \n" +
            "LEFT JOIN ARTIST_TORRENTS_STATUS ATS ON ATS.ARTIST_ID = A.ARTIST_ID")
    List<StatusEntity> getStatuses();

    @Results(id = "songResult", value = {
            @Result(property = "artist", column = "artist"),
            @Result(property = "title", column = "title")
    })
    @Select("SELECT t2.artist, t3.TITLE FROM PLAYLIST_SONGS t1 join ARTISTS t2 on t1.artist_id = t2.artist_id join SONGS t3 on t3.song_id = t1.song_id")
    List<PlaylistSongEntity> getSongs();

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


    @Delete("delete from ARTIST_TORRENTS_STATUS WHERE format = #{format}")
    void deleteAllArtistTorrent(@Param("format") String format);

    @Insert("insert into ARTIST_TORRENTS_STATUS values (#{artistId}, #{torrentId}, #{format}, #{forumId}, #{status})")
    void insertArtistTorrent(@Param("artistId") Integer artistId,
                             @Param("torrentId") String torrentId,
                             @Param("format") String format,
                             @Param("forumId") String forumId,
                             @Param("status") String status);

    @Select("SELECT TORRENT_ID FROM ARTIST_TORRENTS_STATUS WHERE ARTIST_ID = #{artistId} AND FORMAT = #{format} AND STATUS = #{status}")
    List<String> getArtistTorrents(@Param("artistId") Integer artistId,
                                   @Param("format") String format,
                                   @Param("status") String status);

    @Update("UPDATE ARTIST_TORRENTS_STATUS SET STATUS = #{status} WHERE TORRENT_ID = #{torrentId} AND ARTIST_ID = #{artistId} AND FORMAT = #{format}")
    void updateArtistTorrentStatus(@Param("artistId") Integer artistId,
                                   @Param("format") String format,
                                   @Param("torrentId") String torrentId,
                                   @Param("status") String status);
}
