package ru.asm.core.dev.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.dev.model.torrent.TorrentSearcher;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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
                applicationContext.getBean(TorrentSearcher.class)
        );
    }

    @Override
    public void resolveSongSources(Song song) {
        for (Searcher searcher : searchers) {
            searcher.resolveSongSources(song);
        }
    }

    @Override
    public List<TorrentSongSource> getSongSources(Song song) {
        List<TorrentSongSource> resultSources = null;
        for (Searcher searcher : searchers) {
            final List<TorrentSongSource> songSources = searcher.getSongSources(song);
            if (songSources != null) {
                if (resultSources == null) {
                    resultSources = new ArrayList<>();
                }
                resultSources.addAll(songSources);
            }
        }
        return resultSources;
    }

    @Override
    public void downloadSongs(Song song, List<TorrentSongSource> sources) {
        for (Searcher searcher : searchers) {
            searcher.downloadSongs(song, sources);
        }
    }

    @Override
    public List<FileDocument> getDownloadedSongs(Song song) {
        final List<FileDocument> songSources = new ArrayList<>();
        for (Searcher searcher : searchers) {
            songSources.addAll(searcher.getDownloadedSongs(song));
        }
        return songSources;
    }
}
