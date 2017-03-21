package ru.asm.core.index;

import ru.asm.core.index.repositories.TorrentInfoRepository;

/**
 * User: artem.smirnov
 * Date: 16.03.2017
 * Time: 16:31
 */
public interface RepositoryAction {

    void doAction(TorrentInfoRepository repo);
}
