package ru.asm.core;

import com.frostwire.jlibtorrent.TorrentInfo;
import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.ddb.DataStorage;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.progress.TaskProgress;
import ru.asm.core.torrent.TorrentClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 07.11.2017
 * Time: 15:15
 */
abstract class AbstractTorrentFilesSupplier {

    protected DataStorage dataStorage;

    protected TorrentClient torrentClient;

    public AbstractTorrentFilesSupplier(DataStorage dataStorage, TorrentClient torrentClient) {
        this.dataStorage = dataStorage;
        this.torrentClient = torrentClient;
    }

    protected List<SongFileDocument> filterAlreadyExisting2(List<SongFileDocument> requiredSources) {
        final List<SongFileDocument> existingFiles = new ArrayList<>();

        final Iterator<SongFileDocument> it = requiredSources.iterator();
        while (it.hasNext()) {
            final SongFileDocument source = it.next();
            final FileDocument existingFile = dataStorage.getFileBySource(source.getSongSource().getSourceId());
            if (existingFile != null) {
                it.remove();
                existingFiles.add(new SongFileDocument(source.getSong(), source.getSongSource(), existingFile));
            }
        }
        return existingFiles;
    }

    protected FileDocument createFileDocument(Song song, TorrentSongSource requiredSource, File downloadedSong) {
        FileDocument fileDocument = new FileDocument();
        fileDocument.setId(System.nanoTime());
        fileDocument.setFsLocation(downloadedSong.getAbsolutePath());
        fileDocument.setSongId(song.getSongId());
        fileDocument.setSourceId(requiredSource.getSourceId());
        return fileDocument;
    }

    protected TorrentInfo getByMagnet(String magnet) {
        return torrentClient.findByMagnet(magnet);
    }

    protected TorrentClient getTorrentClient() {
        return torrentClient;
    }

    abstract List<SongFileDocument> downloadTorrent(String torrentId,
                                                    String torrentName,
                                                    List<SongFileDocument> requestedSongs,
                                                    TaskProgress taskProgress);
}
