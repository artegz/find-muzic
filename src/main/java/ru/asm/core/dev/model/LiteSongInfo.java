package ru.asm.core.dev.model;

/**
 * User: artem.smirnov
 * Date: 31.08.2017
 * Time: 15:24
 */
public class LiteSongInfo {

    private Song song;
    private int numSources;
    private int numFiles;
    private OperationStatus resolveStatus;

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    public int getNumSources() {
        return numSources;
    }

    public void setNumSources(int numSources) {
        this.numSources = numSources;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    public OperationStatus getResolveStatus() {
        return resolveStatus;
    }

    public void setResolveStatus(OperationStatus resolveStatus) {
        this.resolveStatus = resolveStatus;
    }

}
