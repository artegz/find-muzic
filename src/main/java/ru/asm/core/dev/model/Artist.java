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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artist artist = (Artist) o;

        if (artistId != null ? !artistId.equals(artist.artistId) : artist.artistId != null) return false;
        return artistName != null ? artistName.equals(artist.artistName) : artist.artistName == null;
    }

    @Override
    public int hashCode() {
        int result = artistId != null ? artistId.hashCode() : 0;
        result = 31 * result + (artistName != null ? artistName.hashCode() : 0);
        return result;
    }
}
