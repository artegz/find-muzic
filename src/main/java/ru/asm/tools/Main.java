package ru.asm.tools;

/**
 * User: artem.smirnov
 * Date: 20.03.2017
 * Time: 9:02
 */
public class Main {

    public static void main(String[] args) {
        // Step 1.1: download fresh torrents DB
        new DownloadTorrentsDbTool();
        // todo: extract zip
        // Step 1.2: index downloaded torrents DB
        new IndexTorrentsDbTool();

        // todo
        // Step 2.1: download (fetch) desired playlist
        // Step 2.2: import playlist into DB
        new ImportPlaylistTool();

        // Step 3: resolve artist torrents (find artists in torrents DB)
        new ResolveArtistTorrentsTool();

        // Step 4: index artist songs from torrents
        // search for playlist artist songs in torrents (mp3 and flac files) and index them
        new IndexArtistMp3Tool();
        new IndexArtistFlacTool();

        // Step 5: resolve playlist songs (find desired sons in index)

        // Step 6: download playlist songs

    }
}
