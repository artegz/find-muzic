package ru.asm.tools;

import org.slf4j.LoggerFactory;
import ru.asm.core.index.domain.TorrentFilesVO;
import ru.asm.core.index.domain.TorrentInfoVO;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;

/**
 * User: artem.smirnov
 * Date: 31.03.2017
 * Time: 14:22
 */
class ResolveSongsTask extends ForkJoinTask<TorrentFilesVO> {

    private TorrentFilesVO result = null;

    private String artist;
    private Integer artistId;
    private TorrentInfoVO torrentInfo;

    private String torrentId;

    private Callable<TorrentFilesVO> callable;

    private Throwable error;

    public ResolveSongsTask(String artist, Integer artistId, TorrentInfoVO torrentInfo, String torrentId, Callable<TorrentFilesVO> callable) {
        this.artist = artist;
        this.artistId = artistId;
        this.torrentInfo = torrentInfo;
        this.torrentId = torrentId;
        this.callable = callable;
    }

    public String getArtist() {
        return artist;
    }

    public Integer getArtistId() {
        return artistId;
    }

    public TorrentInfoVO getTorrentInfo() {
        return torrentInfo;
    }

    public String getTorrentId() {
        return torrentId;
    }

    @Override
    public TorrentFilesVO getRawResult() {
        return result;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    protected void setRawResult(TorrentFilesVO value) {
        result = value;
    }

    @Override
    protected boolean exec() {
        try {
            final TorrentFilesVO res = callable.call();
            setRawResult(res);
            return true;
        } catch (Throwable e) {
            LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
            error = e;
            return true;
        }
    }
}
