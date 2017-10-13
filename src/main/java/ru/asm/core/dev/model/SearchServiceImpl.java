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
    public void resolveSongSources(Song song, TaskProgress taskProgress) {
        getTorrentSearcher().resolveSongSources(song, taskProgress);
    }

    @Override
    public void resolveSongSources(Artist artist, List<Song> songs, TaskProgress taskProgress) {
        getTorrentSearcher().resolveSongSources(artist, songs, taskProgress);
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
    public ArtistResolveReport getArtistResolveReport(Artist artist) {
        return getTorrentSearcher().getLastSongResolveReport(artist);
    }

    @Override
    public void downloadSongs(Song song, List<TorrentSongSource> sources, TaskProgress taskProgress) {
//        for (Searcher searcher : searchers) {
//            searcher.downloadSongs(song, sources, async);
//        }
        getTorrentSearcher().downloadSongs(song, sources, taskProgress);
    }

    @Override
    public void downloadSongs(Artist artist, Map<Song, List<TorrentSongSource>> downloadRequest, TaskProgress taskProgress) {
        getTorrentSearcher().downloadSongs(artist, downloadRequest, taskProgress);
    }

    @Override
    public void downloadTorrent(String torrentId, Map<Song, List<TorrentSongSource>> downloadRequest, TaskProgress taskProgress) {
        getTorrentSearcher().downloadTorrent(torrentId, downloadRequest, taskProgress);
    }

    @Override
    public List<FileDocument> getSongDownloadedFiles(Song song) {
//        final List<FileDocument> songSources = new ArrayList<>();
//        for (Searcher searcher : searchers) {
//            songSources.addAll(searcher.getSongDownloadedFiles(song));
//        }
//        return songSources;
        return getTorrentSearcher().getDownloadedSongs(song);
    }
}
