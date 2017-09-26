package ru.asm.core.dev.model;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 26.09.2017
 * Time: 8:46
 */
public class ArtistSongCollection {

    private Artist artist;

    private List<Song> songs;

    public ArtistSongCollection(Artist artist, List<Song> songs) {
        this.artist = artist;
        this.songs = songs;
    }

    public Artist getArtist() {
        return artist;
    }

    public List<Song> getSongs() {
        return songs;
    }
}
