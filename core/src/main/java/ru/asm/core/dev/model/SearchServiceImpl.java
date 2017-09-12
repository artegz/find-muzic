package ru.asm.core.dev.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.dev.model.torrent.TorrentSearcher;
import ru.asm.core.progress.ProgressListener;

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
                getTorrentSearcher()
        );
    }

    private TorrentSearcher getTorrentSearcher() {
        return applicationContext.getBean(TorrentSearcher.class);
    }

    @Override
    public void resolveSongSources(Song song, boolean async, ProgressListener progressListener) {
        getTorrentSearcher().resolveSongSources(song, async, progressListener);
    }

    @Override
    public List<TorrentSongSource> getSongSources(Song song) {
//        List<TorrentSongSource> resultSources = null;
//        for (Searcher searcher : searchers) {
//            final List<TorrentSongSource> songSources = searcher.getSongSources(song);
//            if (songSources != null) {
//                if (resultSources == null) {
//                    resultSources = new ArrayList<>();
//                }
//                resultSources.addAll(songSources);
//            }
//        }
//        return resultSources;
        return getTorrentSearcher().getSongSources(song);
    }

    @Override
    public void downloadSongs(Song song, List<TorrentSongSource> sources, boolean async, ProgressListener progressListener) {
//        for (Searcher searcher : searchers) {
//            searcher.downloadSongs(song, sources, async);
//        }
        getTorrentSearcher().downloadSongs(song, sources, async, progressListener);
    }

    @Override
    public List<FileDocument> getDownloadedSongs(Song song) {
//        final List<FileDocument> songSources = new ArrayList<>();
//        for (Searcher searcher : searchers) {
//            songSources.addAll(searcher.getDownloadedSongs(song));
//        }
//        return songSources;
        return getTorrentSearcher().getDownloadedSongs(song);
    }
}
