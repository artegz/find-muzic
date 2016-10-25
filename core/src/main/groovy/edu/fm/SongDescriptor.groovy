package edu.fm

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType

/**
 * User: artem.smirnov
 * Date: 10.12.2015
 * Time: 12:21
 */
@XmlAccessorType(XmlAccessType.FIELD)
class SongDescriptor implements Comparable<SongDescriptor> {

    String artist

    String title

    SongDescriptor(String artist, String title) {
        this.artist = artist
        this.title = title
    }

    @Override
    int compareTo(SongDescriptor o) {
        return this.toString().compareTo(o.toString())
    }

    @Override
    public String toString() {
        new SongDescriptorMapper().formatSongDescriptor(this)
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        SongDescriptor that = (SongDescriptor) o

        if (artist != that.artist) return false
        if (title != that.title) return false

        return true
    }

    int hashCode() {
        int result
        result = (artist != null ? artist.hashCode() : 0)
        result = 31 * result + (title != null ? title.hashCode() : 0)
        return result
    }
}
