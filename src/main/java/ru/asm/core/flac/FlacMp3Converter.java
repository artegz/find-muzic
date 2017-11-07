package ru.asm.core.flac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.asm.core.AppConfiguration;

import java.io.*;
import java.nio.charset.Charset;

/**
 * User: artem.smirnov
 * Date: 08.09.2017
 * Time: 13:05
 */
public class FlacMp3Converter {

    private static final String MP3SPLT = AppConfiguration.MP3SPLT_EXE_LOCATION;

    private static final Logger logger = LoggerFactory.getLogger(FlacMp3Converter.class);

    private static final Object monitor = new Object();

    public void convert(File directory, String name) {
        synchronized (monitor) {
            try {
                flac2mp3(directory, name);
                mp3splt(directory, name);
            } catch (IOException e) {
                logger.error("conversion of {} - {} to mp3 failed", directory.getAbsolutePath(), name);
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static void flac2mp3(File directory, String name) throws IOException {
        if (new File(directory, name + ".mp3").exists()) {
            return; // already converted into mp3
        }

        ProcessBuilder builder1 = new ProcessBuilder(
                "cmd.exe",
                "/c",
                "flac -cd \"" + name + ".flac\" | lame -h - -v --preset cd \"" + name + ".mp3\""
        );
        builder1.directory(directory);

        builder1.inheritIO();
//        builder1.redirectOutput(new File(directory, "" + name + ".wav"));
        Process p = builder1.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("cp866")));
//        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("utf8")));
        String line;
        while (p.isAlive()) {
            line = r.readLine();
            if (line != null) {
                System.out.println(line);
            } else {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void mp3splt(File directory, String name) throws IOException {
        final File dest = new File(directory, name);

        if (dest.exists() && dest.listFiles() != null && dest.listFiles().length > 0) {
            return; // already split
        }
        if (!dest.exists()) dest.mkdirs();

        ProcessBuilder builder1 = new ProcessBuilder(
                MP3SPLT,
                "-q",
                "-a",
                "-c",
                "\"" + name + ".cue\"",
                "-o",
                "\"@n. @t\"",
                "\"" + name + ".mp3\""
        );
        builder1.directory(directory);

        builder1.inheritIO();
        Process p = builder1.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("cp866")));
//        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("utf8")));
        String line;
        while (p.isAlive()) {
            line = r.readLine();
            if (line != null) {
                System.out.println(line);
            } else {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        final File[] files = directory.listFiles(pathname -> {
            if (pathname.getName().endsWith(".mp3")) {
                if (pathname.getName().matches("[0-9]+.*")) {
                    return true;
                }
            }
            return false;
        });
        if (files != null) {
            for (File file : files) {
                file.renameTo(new File(dest, file.getName()));
            }
        }

        final File tmp = new File(dest, name + ".mp3");
        tmp.deleteOnExit();

    }
}
