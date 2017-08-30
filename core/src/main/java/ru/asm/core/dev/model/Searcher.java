package ru.asm.core.dev.model;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 12:06
 */
public interface Searcher {

    List<SongSource> search(Song song);
}
