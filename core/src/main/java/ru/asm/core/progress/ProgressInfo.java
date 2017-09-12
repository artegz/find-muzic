package ru.asm.core.progress;

import ru.asm.core.persistence.domain.PlaylistSongEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 11:22
 */
public class ProgressInfo {

    private Map<Integer, Task> songProgresses;

    private List<PlaylistSongEntity> songs;

    public ProgressInfo(Collection<PlaylistSongEntity> playlistSongEntities,
                        Map<Integer, Task> songProgresses) {
        this.songProgresses = songProgresses;
        this.songs = new ArrayList<>(playlistSongEntities);
    }

    public Map<Integer, Task> getSongProgresses() {
        return songProgresses;
    }

    public void setSongProgresses(Map<Integer, Task> songProgresses) {
        this.songProgresses = songProgresses;
    }

    public void setSongs(List<PlaylistSongEntity> songs) {
        this.songs = songs;
    }

    public List<PlaylistSongEntity> getSongs() {
        return songs;
    }
}
