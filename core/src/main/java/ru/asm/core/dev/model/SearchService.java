package ru.asm.core.dev.model;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 11:43
 */
public interface SearchService {

    List<SongSource> search(Song song);

    SongResult fetch(List<SongSource> songSources);
}
