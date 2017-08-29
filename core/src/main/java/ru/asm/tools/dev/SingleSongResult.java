package ru.asm.tools.dev;

import ru.asm.core.persistence.domain.PlaylistSongEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 25.08.2017
 * Time: 18:13
 */
public class SingleSongResult {

    private PlaylistSongEntity songEntity;

    private List<TorrentLink> links = new ArrayList<>();

    public SingleSongResult(PlaylistSongEntity songEntity) {
        this.songEntity = songEntity;
    }

    public List<TorrentLink> getLinks() {
        return links;
    }

    public PlaylistSongEntity getSongEntity() {
        return songEntity;
    }
}
