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

        // Step 4: resolve torrents
        //          - groups songs by an artist
        //          - find artist torrents
        //          - [mp3] put file names into DB/index; [flac] parse song names; put them into DB/index
        //          - as output: artist+song - torrent mapping
        //          - for each song choose torrent for downloading
        //          - download torrent
        //          - [mp3] copy required song into result dir; [flac] convert into mp3 and copy required song into result dir
        new ResolveArtistMp3Tool();

        // 2. index artists songs (separate elastic index)
        // ... find files (mp3 + flac)

        // 3. if mp3 => download;
    }
}
