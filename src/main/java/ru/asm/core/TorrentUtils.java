package ru.asm.core;

import com.frostwire.jlibtorrent.Priority;

/**
 * User: artem.smirnov
 * Date: 07.11.2017
 * Time: 11:01
 */
public class TorrentUtils {

    static Priority[] getPrioritiesIgnoreAll(int numFiles) {
        final Priority[] priorities = new Priority[numFiles];
        for (int j = 0; j < numFiles; j++) {
            priorities[j] = Priority.IGNORE;
        }
        return priorities;
    }
}
