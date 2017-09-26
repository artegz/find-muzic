package ru.asm.core.persistence.domain;

/**
 * User: artem.smirnov
 * Date: 20.03.2017
 * Time: 9:17
 */
public class ArtistEntity {

    Integer artistId;

    String artist;

    public ArtistEntity(Integer artistId, String artist) {
        this.artistId = artistId;
        this.artist = artist;
    }

    public Integer getArtistId() {
        return artistId;
    }

    public String getArtist() {
        return artist;
    }
}
