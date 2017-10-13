package ru.asm.core.dev.model;

/**
 * User: artem.smirnov
 * Date: 11.10.2017
 * Time: 9:11
 */
public class LiteArtistInfo {

    private Artist artist;
    private OperationStatus indexingStatus;

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public OperationStatus getIndexingStatus() {
        return indexingStatus;
    }

    public void setIndexingStatus(OperationStatus indexingStatus) {
        this.indexingStatus = indexingStatus;
    }
}
