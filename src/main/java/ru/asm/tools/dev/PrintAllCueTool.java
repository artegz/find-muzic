package ru.asm.tools.dev;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.asm.core.flac.CueParser;
import ru.asm.core.flac.FFileDescriptor;
import ru.asm.core.flac.FTrackDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 07.04.2017
 * Time: 13:30
 */
public class PrintAllCueTool {

    private static final Logger logger = LoggerFactory.getLogger(PrintAllCueTool.class);

    public static void main(String[] args) {
        final File file = new File("C:\\TEMP\\find-music\\downloads\\flac\\");
        try {
            getTorrentSongs(file);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void getTorrentSongs(File torrentDir) throws IOException {
        final Iterator<File> fileIterator = FileUtils.iterateFiles(torrentDir, new String[]{"cue"}, true);
        while (fileIterator.hasNext()) {
            final File cueFile = fileIterator.next();
            parseCue(torrentDir, cueFile);
        }
    }

    private static void parseCue(File torrentDir, File cueFile) throws IOException {
        final List<FFileDescriptor> fFileDescriptors = new CueParser().parseCue(torrentDir, cueFile);
        for (FFileDescriptor fFileDescriptor : fFileDescriptors) {
            logger.info(String.format("%s || %s ", fFileDescriptor.getPerformer(), fFileDescriptor.getTitle()));
            for (FTrackDescriptor trackDescriptor : fFileDescriptor.getTrackDescriptors()) {
                logger.info(String.format("   %s || %s ", trackDescriptor.getPerformer(), trackDescriptor.getTitle()));
            }
        }
    }
}
