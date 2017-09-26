package ru.asm.core.dev.model.ddb;

import org.dizitart.no2.objects.Id;

/**
 * User: artem.smirnov
 * Date: 31.08.2017
 * Time: 15:37
 */
public class FileDocument {

    @Id
    private Long id;

    private Integer songId;

    private String sourceId;

    private String fsLocation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSongId() {
        return songId;
    }

    public void setSongId(Integer songId) {
        this.songId = songId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getFsLocation() {
        return fsLocation;
    }

    public void setFsLocation(String fsLocation) {
        this.fsLocation = fsLocation;
    }
}
