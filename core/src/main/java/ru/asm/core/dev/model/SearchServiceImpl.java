package ru.asm.core.dev.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
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
    public List<SongSource> search(Song song) {
        final List<SongSource> songSources = new ArrayList<>();
        for (Searcher searcher : searchers) {
            songSources.addAll(searcher.search(song));
        }
        return songSources;
    }

    @Override
    public SongResult fetch(List<SongSource> songSources) {
        return null;
    }
}
