package ru.asm.core.dev.model;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 31.08.2017
 * Time: 15:30
 */
public class DownloadInfo {

    private List<String> sourcesIds;

    public void setSourcesIds(List<String> sourcesIds) {
        this.sourcesIds = sourcesIds;
    }

    public List<String> getSourcesIds() {
        return sourcesIds;
    }
}
