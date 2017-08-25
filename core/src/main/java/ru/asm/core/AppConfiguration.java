package ru.asm.core;

import java.io.File;

/**
 * User: artem.smirnov
 * Date: 25.08.2017
 * Time: 15:42
 */
public class AppConfiguration {

    private static final AppConfiguration instance = new AppConfiguration();

    private File torrentDbsStorageLocation = new File("C:\\TEMP\\find-music\\rutracker_org_db");

    public static AppConfiguration getInstance() {
        return instance;
    }

    public File getTorrentDbsStorageLocation() {
        return torrentDbsStorageLocation;
    }
}
