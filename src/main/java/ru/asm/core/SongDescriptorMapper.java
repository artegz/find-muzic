package ru.asm.core;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: artem.smirnov
 * Date: 10.12.2015
 * Time: 12:21
 */
public class SongDescriptorMapper {

    public Set<SongDescriptor> parseList(Collection<String> songs) {
        TreeSet<SongDescriptor> result = new TreeSet<SongDescriptor>();
        for (String song : songs) {
            result.add(parseSongDescriptor(song));
        }
        return result;
    }

    public Set<String> formatList(Collection<SongDescriptor> songs) {
        TreeSet<String> result = new TreeSet<String>();
        for (SongDescriptor song : songs) {
            result.add(formatSongDescriptor(song));
        }
        return result;
    }

    public SongDescriptor parseSongDescriptor(String songFullName) {
        String[] artistTitle = songFullName.split(" - ");
        if (artistTitle.length != 2) {
            throw new IllegalArgumentException("unable to parse '${songFullName}'");
        }
        return new SongDescriptor(artistTitle[0], artistTitle[1]);
    }

    public String formatSongDescriptor(SongDescriptor sd) {
        return "${sd.artist} - ${sd.title}";
    }
}
