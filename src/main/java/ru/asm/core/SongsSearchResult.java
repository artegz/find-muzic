package ru.asm.core;

import ru.asm.tools.dev.SingleSongResult;

import java.util.ArrayList;

/**
 * User: artem.smirnov
 * Date: 25.08.2017
 * Time: 18:13
 */
public class SongsSearchResult {

    final ArrayList<SingleSongResult> found = new ArrayList<>();
    final ArrayList<SingleSongResult> notFound = new ArrayList<>();

    public ArrayList<SingleSongResult> getFound() {
        return found;
    }

    public ArrayList<SingleSongResult> getNotFound() {
        return notFound;
    }
}
