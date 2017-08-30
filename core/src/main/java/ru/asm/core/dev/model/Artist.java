package ru.asm.core.dev.model;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 11:28
 */
public class Artist {

    private Integer artistId;

    private String artistName;

    public Integer getArtistId() {
        return artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
}
