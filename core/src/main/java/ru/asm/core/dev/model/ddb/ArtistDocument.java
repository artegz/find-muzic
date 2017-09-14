package ru.asm.core.dev.model.ddb;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 15:21
 */
public class ArtistDocument {

    private Integer artistId;

    private String artistName;

    private List<String> artistTorrentIds;

    public Integer getArtistId() {
        return artistId;
    }

    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public List<String> getArtistTorrentIds() {
        return artistTorrentIds;
    }

    public void setArtistTorrentIds(List<String> artistTorrentIds) {
        this.artistTorrentIds = artistTorrentIds;
    }
}
