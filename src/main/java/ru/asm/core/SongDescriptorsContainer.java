package ru.asm.core;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

/**
 * User: artem.smirnov
 * Date: 25.10.2016
 * Time: 15:11
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SongDescriptorsContainer {

    private Collection<SongDescriptor> songDescriptors;

    public SongDescriptorsContainer(Collection<SongDescriptor> songDescriptors) {
        this.songDescriptors = songDescriptors;
    }

    public Collection<SongDescriptor> getSongDescriptors() {
        return songDescriptors;
    }

    public void setSongDescriptors(Collection<SongDescriptor> songDescriptors) {
        this.songDescriptors = songDescriptors;
    }
}
