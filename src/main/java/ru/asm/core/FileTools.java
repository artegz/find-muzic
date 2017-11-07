package ru.asm.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 06.11.2015
 * Time: 11:00
 */
public class FileTools {

    private static final Logger log = LoggerFactory.getLogger(FileTools.class);

    public static List<SongDescriptor> readCsv(File songsFile) {
        List<SongDescriptor> res = new ArrayList<>();

        if (songsFile.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(songsFile))) {
                String it;
                while ((it = r.readLine()) != null) {
                    String[] parts = it.split(";");

                    String artist = parts[0];
                    String title = parts[1];

                    if (artist.startsWith("\"")) {
                        // unwrap from "exit
                        res.add(new SongDescriptor(artist.substring(1, artist.length() - 1), title.substring(1, title.length() - 1)));
                    } else {
                        res.add(new SongDescriptor(artist, title));
                    }
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.error("${songsFile.getName()} doesn't exist");
        }
        return new ArrayList<>(res);
    }
}
