package ru.asm.core.dev.model;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 11:28
 */
public class Song {

    private Integer songId;

    private String title;

    private Artist artist;

    public Integer getSongId() {
        return songId;
    }

    public String getTitle() {
        return title;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setSongId(Integer songId) {
        this.songId = songId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Song song = (Song) o;

        if (songId != null ? !songId.equals(song.songId) : song.songId != null) return false;
        if (title != null ? !title.equals(song.title) : song.title != null) return false;
        return artist != null ? artist.equals(song.artist) : song.artist == null;
    }

    @Override
    public int hashCode() {
        int result = songId != null ? songId.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (artist != null ? artist.hashCode() : 0);
        return result;
    }

    public String getFullName() {
        return getArtist().getArtistName() + " " + getTitle();
    }
}
