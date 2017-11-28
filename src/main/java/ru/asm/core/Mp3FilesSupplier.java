package ru.asm.core;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.ddb.DataStorage;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.ddb.TorrentDocument;
import ru.asm.core.progress.TaskProgress;
import ru.asm.core.torrent.TorrentClient;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: artem.smirnov
 * Date: 07.11.2017
 * Time: 15:17
 */
class Mp3FilesSupplier extends AbstractTorrentFilesSupplier {

    private static final Logger logger = LoggerFactory.getLogger(Mp3FilesSupplier.class);

    public Mp3FilesSupplier(DataStorage dataStorage, TorrentClient torrentClient) {
        super(dataStorage, torrentClient);
    }

    @Override
    List<SongFileDocument> downloadTorrent(String torrentId,
                                                   String torrentName,
                                                   List<SongFileDocument> requestedSongs,
                                                   TaskProgress taskProgress) {
        final List<SongFileDocument> resultSongsWithFile = new ArrayList<>();

        final List<SongFileDocument> notDownloadedRequestedSongs = new ArrayList<>(requestedSongs);
        final List<SongFileDocument> alreadyDownloadedRequestedSongsWithFile = filterAlreadyExisting2(notDownloadedRequestedSongs);

        final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

        final List<SongFileDocument> downloadedRequestedSongsWithFile;
        if (!notDownloadedRequestedSongs.isEmpty()) {
            final List<SongFileDocument> downloadedSongsWithFile = new ArrayList<>();

            taskProgress.log("resolving magnet...");
            final String magnet = torrent.getTorrentInfo().getMagnet();
            logger.info("resoling torrent {} info by magnet {}", torrentId, magnet);
            TorrentInfo torrentInfo = getByMagnet(magnet);

            final Set<String> requiredFilesPaths = getRequiredMp3FilePaths2(notDownloadedRequestedSongs);

            if (!requiredFilesPaths.isEmpty()) {

                taskProgress.log("downloading files...");
                logger.info("[{}] magnet resolved, downloading '{}'", torrentName, requiredFilesPaths);
                final Priority[] priorities = getRequiredFilesPriorities(torrentInfo, requiredFilesPaths);
                final File saveDir = new File(new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE), torrentId);
                if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                    saveDir.mkdirs();
                getTorrentClient().download(torrentInfo, saveDir, priorities, taskProgress);

                taskProgress.log("saving downloaded files...");
                for (SongFileDocument notDownloadedRequestedSong : notDownloadedRequestedSongs) {
                    final Song song = notDownloadedRequestedSong.getSong();

                    final String mp3FilePath = notDownloadedRequestedSong.getSongSource().getIndexSong().getMp3FilePath();
                    if (mp3FilePath == null) {
                        continue;
                    }

                    final File downloadedSongFile = new File(saveDir, mp3FilePath);
                    if (downloadedSongFile.exists()) {
                        final FileDocument fileDocument = createFileDocument(song, notDownloadedRequestedSong.getSongSource(), downloadedSongFile);

                        dataStorage.insertFile(fileDocument);
                        logger.info("[{}: {}] download complete", torrentName, song.getTitle(), mp3FilePath);

                        downloadedSongsWithFile.add(new SongFileDocument(song, notDownloadedRequestedSong.getSongSource(), fileDocument));
                    } else {
                        logger.warn("[{}: {}] download failed", torrentName, song.getTitle(), mp3FilePath);
                    }
                }

            } else {
                logger.warn("nothing to download (torrent {})", torrentId);
            }

            downloadedRequestedSongsWithFile = downloadedSongsWithFile;
            downloadedRequestedSongsWithFile.addAll(alreadyDownloadedRequestedSongsWithFile);
        } else {
            downloadedRequestedSongsWithFile = alreadyDownloadedRequestedSongsWithFile;
        }

        resultSongsWithFile.addAll(downloadedRequestedSongsWithFile);

        return resultSongsWithFile;
    }

    private Set<String> getRequiredMp3FilePaths2(List<SongFileDocument> requiredSources) {
        final Set<String> requiredFiles = new HashSet<>();
        for (SongFileDocument requiredSource : requiredSources) {
            if (requiredSource.getSongSource().getIndexSong().getMp3FilePath() != null) {
                requiredFiles.add(requiredSource.getSongSource().getIndexSong().getMp3FilePath());
            }
        }
        return requiredFiles;
    }

    private Priority[] getRequiredFilesPriorities(TorrentInfo torrentInfo, Set<String> requiredFiles) {
        final FileStorage files = torrentInfo.files();
        final Priority[] priorities = TorrentUtils.getPrioritiesIgnoreAll(files.numFiles());

        for (int i = 0; i < files.numFiles(); i++) {
            if (requiredFiles.contains(files.filePath(i))) {
                priorities[i] = Priority.NORMAL;
            }
        }
        return priorities;
    }
}
