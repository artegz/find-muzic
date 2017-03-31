package ru.asm.tools;

/**
 * User: artem.smirnov
 * Date: 20.03.2017
 * Time: 9:02
 */
public class Main {

    public static void main(String[] args) {
        // Step 1: download fresh torrents DB
        new TorrentsDbDownloadTool();

        // Step 2: index downloaded torrents DB
        new TorrentsDbIndexTool();

        // Step 2.1: print categories
        new TorrentsDbPrintCategoriesTool();

        // Step 3: fetch desired playlist
        // todo

        // Step 3.1: import playlist into DB
        new ImportPlaylistTool();


        // Step 3.9: resolve artist torrents
        new ResolveArtistTorrentsTool();

        // Step 4: resolve torrents
        // search for playlist artist songs in torrents (mp3 and flac files) and index them
        new ResolveArtistMp3Tool();
        new ResolveArtistFlacTool();

        // 2. index artists songs (separate elastic index)
        // ... find files (mp3 + flac)

        // 3. if mp3 => download;
    }
}
