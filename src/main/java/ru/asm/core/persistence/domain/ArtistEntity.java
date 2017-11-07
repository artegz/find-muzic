package ru.asm.core.persistence.domain;

/**
 * User: artem.smirnov
 * Date: 20.03.2017
 * Time: 9:17
 */
public class ArtistEntity {

    private Integer artistId;

    private String artist;

    public ArtistEntity() {
    }

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

    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
