package ru.asm.core;

import java.io.File;

/**
 * User: artem.smirnov
 * Date: 25.08.2017
 * Time: 15:42
 */
public class AppConfiguration {

    public static final boolean DOWNLOAD_ALLOWED = true;

    private static final AppConfiguration instance = new AppConfiguration();

    /*public static final String TORRENTS_BACKUP_DOWNLOAD_LOCATION = "F:\\TEMP\\find-music\\xml\\rutracker_org_db";
    public static final String TORRENTS_BACKUP_XML_LOCATION = "F:\\TEMP\\find-music\\xml\\rutracker_org_db\\backup.20170208185701.xml";
    public static final String ES_HOME_LOCATION = "F:\\TEMP\\find-music\\es\\es_home";
    public static final String ES_DATA_LOCATION = "F:\\TEMP\\find-music\\es\\es_data";
    public static final String H2_DB_FILE_LOCATION = "F:\\TEMP\\find-music\\h2db\\h2db";
    public static final String N2O_DB_FILE_LOCATION = "F:\\TEMP\\find-music\\n2odb";

    public static final String JLIBTORRENT_DLL_LOCATION = "C:\\CmdTools\\lib\\x86_64\\jlibtorrent.dll";
    public static final String MP3SPLT_EXE_LOCATION = "C:\\CmdTools\\mp3splt\\mp3splt.exe";

    public static final String DOWNLOADED_SONGS_STORAGE = "F:\\TEMP\\find-music\\storage";
    public static final String FLAC_DOWNLOAD_TEMP_DIR = DOWNLOADED_SONGS_STORAGE;

    public static final String DEFAULT_PLAYLIST_FILE_LOCATION = "E:\\IdeaProjects\\find-muzic\\core\\src\\main\\resources\\playlists\\102015.nashe.playlist.txt";*/

    public static final String TORRENTS_BACKUP_DOWNLOAD_LOCATION = "C:\\TEMP\\find-music\\rutracker_org_db\\backup.20170208185701";
    public static final String TORRENTS_BACKUP_XML_LOCATION = "C:\\TEMP\\find-music\\rutracker_org_db\\backup.20170208185701\\backup.20170208185701.xml";
    public static final String ES_HOME_LOCATION = "C:\\TEMP\\find-music\\es_data";
    public static final String ES_DATA_LOCATION = "C:\\TEMP\\find-music\\es_data";
    public static final String H2_DB_FILE_LOCATION = "C:\\TEMP\\find-music\\h2\\fmdb";
    public static final String N2O_DB_FILE_LOCATION = "C:\\TEMP\\find-music\\n2o\\test.db";

    public static final String JLIBTORRENT_DLL_LOCATION = "C:\\TEMP\\find-music\\jlibtorrent\\jlibtorrent-windows-1.2.0.6\\lib\\x86_64\\jlibtorrent.dll";
    public static final String MP3SPLT_EXE_LOCATION = "C:\\CommanLineTools\\mp3splt\\mp3splt.exe";

    public static final String DOWNLOADED_SONGS_STORAGE = "C:\\TEMP\\find-music\\storage";
    public static final String FLAC_DOWNLOAD_TEMP_DIR = "C:\\TEMP\\find-music\\downloads\\flac\\";

    public static final String DEFAULT_PLAYLIST_FILE_LOCATION = "C:\\IdeaProjects\\find-muzic\\src\\main\\resources\\playlists/test.nashe.playlist.txt";


    public static AppConfiguration getInstance() {
        return instance;
    }

    public static int getFetchMagnetTimeout() {
        return 60;
    }

    public File getTorrentBackupDownloadFile() {
        return new File(TORRENTS_BACKUP_DOWNLOAD_LOCATION);
    }
}
