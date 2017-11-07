package ru.asm.core.dev.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.dev.model.torrent.TorrentSearcher;
import ru.asm.core.progress.TaskProgress;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 12:06
 */
@Component
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ApplicationContext applicationContext;

    private List<Searcher> searchers = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        searchers.add(
                getTorrentSearcher()
        );
    }

    private TorrentSearcher getTorrentSearcher() {
        return applicationContext.getBean(TorrentSearcher.class);
    }

    @Override
    public void indexArtist(Artist artist, TaskProgress taskProgress) {
        getTorrentSearcher().indexArtist(artist, taskProgress);
    }

    @Override
    public void searchSong(Artist artist, Song song, TaskProgress taskProgress) {
        getTorrentSearcher().searchSong(artist, song, taskProgress);
    }

    @Override
    public List<TorrentSongSource> getSongSources(Song song) {
        return getTorrentSearcher().getSongSources(song);
    }

    @Override
    public ArtistResolveReport getArtistResolveReport(Artist artist) {
        return getTorrentSearcher().getLastSongResolveReport(artist);
    }

    @Override
    public void downloadTorrent(String torrentId, Map<Song, List<TorrentSongSource>> downloadRequest, TaskProgress taskProgress) {
        getTorrentSearcher().downloadTorrent(torrentId, downloadRequest, taskProgress);
    }

    @Override
    public List<FileDocument> getSongDownloadedFiles(Song song) {
        return getTorrentSearcher().getDownloadedSongs(song);
    }
}
