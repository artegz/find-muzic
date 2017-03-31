package ru.asm.core.flac;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 24.03.2017
 * Time: 14:25
 */
public class FFileDescriptor {

    // PERFORMER "5'nizza"
    private String performer;

    // TITLE "Пятница"
    private String title;

    // FILE "5'nizza - Пятница.flac" WAVE
    private String file;
    private String fileType;

    private List<FTrackDescriptor> trackDescriptors;

    private String relativePath;

    private String fileName;

    public FFileDescriptor(String relativePath, String fileName) {
        this.relativePath = relativePath;
        this.fileName = fileName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public List<FTrackDescriptor> getTrackDescriptors() {
        return trackDescriptors;
    }

    public void setTrackDescriptors(List<FTrackDescriptor> trackDescriptors) {
        this.trackDescriptors = trackDescriptors;
    }

    public String getPerformer() {
        return performer;
    }

    public void setPerformer(String performer) {
        this.performer = performer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
