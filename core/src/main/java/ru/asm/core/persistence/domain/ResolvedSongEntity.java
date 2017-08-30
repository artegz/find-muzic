package ru.asm.core.persistence.domain;

/**
 * User: artem.smirnov
 * Date: 29.08.2017
 * Time: 16:41
 */
public class ResolvedSongEntity extends PlaylistSongEntity implements Comparable<ResolvedSongEntity> {

    private String torrentId;

    private String fileId;

    public String getTorrentId() {
        return torrentId;
    }

    public void setTorrentId(String torrentId) {
        this.torrentId = torrentId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public int compareTo(ResolvedSongEntity o) {
        return fileId.compareTo(o.fileId);
    }
}
