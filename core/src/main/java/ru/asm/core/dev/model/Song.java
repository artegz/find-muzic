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
}
