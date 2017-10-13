package ru.asm.core.dev.model;

import ru.asm.util.ResolveStatuses;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 13.09.2017
 * Time: 9:18
 */
public class ArtistResolveReport {

    private Integer songId;
    private Integer artistId;

    private Date startTime;
    private Date endTime;

    private Boolean resolvePerformed = false;
    private Status resolveStatus;
    private String resolveFailureReason = null;
    private List<String> resolvedTorrentIds;

    private Boolean indexingPerformed = false;
    private Map<String, String> torrentsIndexingStatuses = null;

    private Boolean searchPerformed;
    private List<String> foundSources;

    public ArtistResolveReport() {
    }

    public ArtistResolveReport(Integer artistId) {
        this.songId = songId;
        this.artistId = artistId;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setResolveStatus(Status resolveStatus) {
        this.resolveStatus = resolveStatus;
    }

    public void setResolveFailureReason(String resolveFailureReason) {
        this.resolveFailureReason = resolveFailureReason;
    }

    public void setResolvedTorrentIds(List<String> resolvedTorrentIds) {
        this.resolvedTorrentIds = resolvedTorrentIds;
    }

    public void setTorrentIndexingStatus(String torrentId, String status) {
        if (torrentsIndexingStatuses == null) {
            torrentsIndexingStatuses = new HashMap<>();
        }
        torrentsIndexingStatuses.put(torrentId, status);
    }

    public void setResolvePerformed(Boolean resolvePerformed) {
        this.resolvePerformed = resolvePerformed;
    }

    public void setIndexingPerformed(Boolean indexingPerformed) {
        this.indexingPerformed = indexingPerformed;
    }

    public void setSearchPerformed(Boolean searchPerformed) {
        this.searchPerformed = searchPerformed;
    }

    public void setFoundSources(List<String> foundSources) {
        this.foundSources = foundSources;
    }

    public boolean isResolveSucceeded() {
        return resolvePerformed && resolveStatus == Status.success;
    }

    public boolean isIndexingSucceeded() {
        return indexingPerformed
                && !torrentsIndexingStatuses.isEmpty()
                && torrentsIndexingStatuses.containsValue(ResolveStatuses.STATUS_OK);
    }

    public Integer getSongId() {
        return songId;
    }

    public Integer getArtistId() {
        return artistId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public Boolean getResolvePerformed() {
        return resolvePerformed != null && resolvePerformed;
    }

    public Status getResolveStatus() {
        return resolveStatus;
    }

    public String getResolveFailureReason() {
        return resolveFailureReason;
    }

    public List<String> getResolvedTorrentIds() {
        return resolvedTorrentIds;
    }

    public Boolean getIndexingPerformed() {
        return indexingPerformed != null && indexingPerformed;
    }

    public Map<String, String> getTorrentsIndexingStatuses() {
        return torrentsIndexingStatuses;
    }

    public Boolean getSearchPerformed() {
        return searchPerformed != null && searchPerformed;
    }

    public List<String> getFoundSources() {
        return foundSources;
    }

    public static enum Status {
        success,
        failed
    }
}
