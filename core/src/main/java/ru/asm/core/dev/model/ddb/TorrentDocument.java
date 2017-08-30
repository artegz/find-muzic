package ru.asm.core.dev.model.ddb;

import org.dizitart.no2.objects.Id;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.domain.TorrentSongVO;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 15:22
 */
public class TorrentDocument {

    @Id
    private Long id;

    private String torrentId;

    private TorrentInfoVO torrentInfo;

    private String format;

    private String status;

    private List<TorrentSongVO> torrentSongs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTorrentId() {
        return torrentId;
    }

    public void setTorrentId(String torrentId) {
        this.torrentId = torrentId;
    }

    public TorrentInfoVO getTorrentInfo() {
        return torrentInfo;
    }

    public void setTorrentInfo(TorrentInfoVO torrentInfo) {
        this.torrentInfo = torrentInfo;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<TorrentSongVO> getTorrentSongs() {
        return torrentSongs;
    }

    public void setTorrentSongs(List<TorrentSongVO> torrentSongs) {
        this.torrentSongs = torrentSongs;
    }
}
