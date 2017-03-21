package ru.asm.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

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

    static File getDir(String dirName) {
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    static List<String> getLoadedSongs(File workDir) {
        ArrayList<String> result = new ArrayList<String>();
        SuffixFileFilter filter = new SuffixFileFilter("mp3", IOCase.INSENSITIVE);
        for (File mp3File : workDir.listFiles((FileFilter) filter)) {
            result.add(mp3File.getName().replace(".mp3", ""));
        }
        return result;
    }

    static List<String> readSongs(File workDir, String filename) {
        TreeSet<String> res = new TreeSet<String>();
        File songsFile = new File(workDir, filename);
        if (songsFile.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(songsFile))) {
                String it;
                while ((it = r.readLine()) != null) {
                    res.add(it);
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.error("${songsFile.getName()} doesn't exist");
        }
        return new ArrayList<>(res);
    }

    static void writeMapping(File workDir, Map<String, String> songs, String filename, boolean override) {
        File songsFile = new File(workDir, filename);
        try {
            if (override) {
                if (songsFile.exists()) {
                    songsFile.delete();
                }
                songsFile.createNewFile();
            } else if (!songsFile.exists()) {
                songsFile.createNewFile();
            }

            try (PrintStream out = new PrintStream(songsFile)) {
                for (Map.Entry<String, String> entry : songs.entrySet()) {
                    out.println(entry.getKey() + "\t" + entry.getValue());
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            if (override) {
    //            try (PrintStream out = new PrintStream(songsFile)) {
    //                for (Map.Entry<String, String> entry : songs.entrySet()) {
    //                    out.println(entry.getKey() + "\t" + entry.getValue());
    //                }
    //            } catch (Throwable e) {
    //                log.error(e.getMessage(), e);
    //            }
            } else {
    //            songsFile.withWriterAppend { out ->
    //                songs.each {
    //                    out.println it.key + "\t" + it.value
    //                }
    //            }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }

    static void writeSongsJson(File workDir, Collection<SongDescriptor> songs, String filename, boolean override) {
        ObjectMapper mapper = getObjectMapper();

        try {
            File songsFile = new File(workDir, filename);
            if (override) {
                if (songsFile.exists()) {
                    songsFile.delete();
                }
                songsFile.createNewFile();
            } else if (!songsFile.exists()) {
                songsFile.createNewFile();
            }

            SongDescriptorsContainer container = new SongDescriptorsContainer(songs);

            try (PrintStream out = new PrintStream(songsFile)) {
                mapper.writeValue(out, container);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
//            if (override) {
//                songsFile.withWriter { out ->
//                    mapper.writeValue(out, container)
//                }
//            } else {
//                songsFile.withWriterAppend { out ->
//                    mapper.writeValue(out, container)
//                }
//            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static ObjectMapper getObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        return objectMapper;
    }

    static void writeSongs(File workDir, Collection<String> songs, String filename, boolean override) {
        File songsFile = new File(workDir, filename);
        try {
            if (override) {
                if (songsFile.exists()) {
                    songsFile.delete();
                }
                songsFile.createNewFile();
            } else if (!songsFile.exists()) {
                songsFile.createNewFile();
            }

            try (PrintStream out = new PrintStream(songsFile)) {
                for (String song : songs) {
                    out.println(song);
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
//        if (override) {
//            songsFile.withWriter { out ->
//                songs.each {
//                    out.println it
//                }
//            }
//        } else {
//            songsFile.withWriterAppend { out ->
//                songs.each {
//                    out.println it
//                }
//            }
//        }
    }

    static File getSubDir(File workDir, String subDirName) {
     File resultFolder = new File(workDir, subDirName);
     if (!resultFolder.exists()) resultFolder.mkdirs();
     return resultFolder;
 }
}
