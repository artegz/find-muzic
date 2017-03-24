package ru.asm.core.persistence.domain;

/**
 * User: artem.smirnov
 * Date: 15.06.2016
 * Time: 10:28
 */
public class PlaylistSongEntity {

    private String artist;

    private String title;

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("");
        sb.append("").append(artist).append(": ");
        sb.append("").append(title).append("");
        sb.append("");
        return sb.toString();
    }
}
