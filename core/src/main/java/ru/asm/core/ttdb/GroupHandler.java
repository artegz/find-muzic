package ru.asm.core.ttdb;

import ru.asm.core.index.domain.TorrentInfoVO;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 05.04.2017
 * Time: 9:41
 */
public interface GroupHandler {

    void handleGroup(List<TorrentInfoVO> torrentInfos);
}
