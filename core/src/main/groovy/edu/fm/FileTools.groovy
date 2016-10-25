package edu.fm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOCase
import org.apache.commons.io.filefilter.SuffixFileFilter
/**
 * User: artem.smirnov
 * Date: 06.11.2015
 * Time: 11:00
 */
@Slf4j
class FileTools {

    static List<SongDescriptor> readCsv(File songsFile) {
        def res = new TreeSet<SongDescriptor>()

        if (songsFile.exists()) {
            songsFile.withReader { r ->
                r.eachLine {
                    def parts = it.split(";")

                    def artist = parts[0]
                    def title = parts[1]

                    if (artist.startsWith("\"")) {
                        // unwrap from "exit
                        res.add(new SongDescriptor(artist.substring(1, artist.size() - 1), title.substring(1, title.size() - 1)))
                    } else {
                        res.add(new SongDescriptor(artist, title))
                    }
                }
            }
        } else {
            log.error("${songsFile.getName()} doesn't exsist")
        }
        new ArrayList<>(res)
    }

    static File getDir(String dirName) {
        def dir = new File(dirName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    static List<String> getLoadedSongs(File workDir) {
        def result = new ArrayList<String>()
        def filter = new SuffixFileFilter("mp3", IOCase.INSENSITIVE)
        for (File mp3File : workDir.listFiles((FileFilter) filter)) {
            result.add(mp3File.getName().replace(".mp3", ""))
        }
        result
    }

    static List<String> readSongs(File workDir, String filename) {
        def res = new TreeSet<String>()
        def songsFile = new File(workDir, filename)
        if (songsFile.exists()) {
            songsFile.withReader { r ->
                r.eachLine {
                    res.add(it)
                }
            }
        } else {
            log.error("${songsFile.getName()} doesn't exsist")
        }
        new ArrayList<>(res)
    }

    static void writeMapping(File workDir, Map<String, String> songs, String filename, boolean override) {
        def songsFile = new File(workDir, filename)
        if (override) {
            if (songsFile.exists()) {
                songsFile.delete()
            }
            songsFile.createNewFile()
        } else if (!songsFile.exists()) {
            songsFile.createNewFile();
        }

        if (override) {
            songsFile.withWriter { out ->
                songs.each {
                    out.println it.key + "\t" + it.value
                }
            }
        } else {
            songsFile.withWriterAppend { out ->
                songs.each {
                    out.println it.key + "\t" + it.value
                }
            }
        }

    }

    static void writeSongsJson(File workDir, Collection<SongDescriptor> songs, String filename, boolean override) {
        def mapper = getObjectMapper()

        def songsFile = new File(workDir, filename)
        if (override) {
            if (songsFile.exists()) {
                songsFile.delete()
            }
            songsFile.createNewFile()
        } else if (!songsFile.exists()) {
            songsFile.createNewFile();
        }

        def container = new SongDescriptorsContainer(songs)

        if (override) {
            songsFile.withWriter { out ->
                mapper.writeValue(out, container)
            }
        } else {
            songsFile.withWriterAppend { out ->
                mapper.writeValue(out, container)
            }
        }
    }

    private static ObjectMapper getObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        return objectMapper;
    }

    static void writeSongs(File workDir, Collection<String> songs, String filename, boolean override) {
        def songsFile = new File(workDir, filename)
        if (override) {
            if (songsFile.exists()) {
                songsFile.delete()
            }
            songsFile.createNewFile()
        } else if (!songsFile.exists()) {
            songsFile.createNewFile();
        }

        if (override) {
            songsFile.withWriter { out ->
                songs.each {
                    out.println it
                }
            }
        } else {
            songsFile.withWriterAppend { out ->
                songs.each {
                    out.println it
                }
            }
        }
    }

    static File getSubDir(File workDir, String subDirName) {
     File resultFolder = new File(workDir, subDirName)
     if (!resultFolder.exists()) resultFolder.mkdirs()
     resultFolder
 }
}
