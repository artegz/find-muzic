package edu.fm

/**
 * User: artem.smirnov
 * Date: 10.12.2015
 * Time: 12:21
 */
class SongDescriptorMapper {

    Set<SongDescriptor> parseList(Collection<String> songs) {
        def result = new TreeSet<SongDescriptor>()
        for (String song : songs) {
            result.add(parseSongDescriptor(song))
        }
        result
    }

    Set<String> formatList(Collection<SongDescriptor> songs) {
        def result = new TreeSet<String>()
        for (SongDescriptor song : songs) {
            result.add(formatSongDescriptor(song))
        }
        result
    }

    SongDescriptor parseSongDescriptor(String songFullName) {
        def artistTitle = songFullName.split(" - ")
        if (artistTitle.length != 2) {
            throw new IllegalArgumentException("unable to parse '${songFullName}'")
        }
        new SongDescriptor(artistTitle[0], artistTitle[1])
    }

    String formatSongDescriptor(SongDescriptor sd) {
        return "${sd.artist} - ${sd.title}"
    }
}
