package ru.asm.core.flac;

/**
 * User: artem.smirnov
 * Date: 24.03.2017
 * Time: 14:26
 */
public class FTrackDescriptor {

    // TRACK 01 AUDIO
    private String trackNum;
    private String trackType;

    // TITLE "Сюрная"
    private String title;

    // PERFORMER "5'nizza"
    private String performer;

    // INDEX 01 00:00:00
    private String indexNum;
    private String indexTime;

    public FTrackDescriptor() {
    }

    public String getTrackNum() {
        return trackNum;
    }

    public void setTrackNum(String trackNum) {
        this.trackNum = trackNum;
    }

    public String getTrackType() {
        return trackType;
    }

    public void setTrackType(String trackType) {
        this.trackType = trackType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPerformer() {
        return performer;
    }

    public void setPerformer(String performer) {
        this.performer = performer;
    }

    public String getIndexNum() {
        return indexNum;
    }

    public void setIndexNum(String indexNum) {
        this.indexNum = indexNum;
    }

    public String getIndexTime() {
        return indexTime;
    }

    public void setIndexTime(String indexTime) {
        this.indexTime = indexTime;
    }
}
