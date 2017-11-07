package ru.asm.core;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * User: artem.smirnov
 * Date: 10.12.2015
 * Time: 12:21
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SongDescriptor implements Comparable<SongDescriptor> {

    private String artist;

    private String title;

    SongDescriptor(String artist, String title) {
        this.artist = artist;
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int compareTo(SongDescriptor o) {
        return this.toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return "${sd.artist} - ${sd.title}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SongDescriptor that = (SongDescriptor) o;

        if (artist != null ? !artist.equals(that.artist) : that.artist != null) return false;
        return title != null ? title.equals(that.title) : that.title == null;
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        return result;
    }
}
