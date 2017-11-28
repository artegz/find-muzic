package ru.asm.core;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.asm.core.dev.model.Song;
import ru.asm.core.dev.model.ddb.DataStorage;
import ru.asm.core.dev.model.ddb.FileDocument;
import ru.asm.core.dev.model.ddb.SongSourceDocument;
import ru.asm.core.dev.model.ddb.TorrentDocument;
import ru.asm.core.dev.model.torrent.TorrentSongSource;
import ru.asm.core.flac.FlacMp3Converter;
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
 * Time: 15:16
 */
class FlacFilesSupplier extends AbstractTorrentFilesSupplier {

    private static final Logger logger = LoggerFactory.getLogger(FlacFilesSupplier.class);

    public FlacFilesSupplier(DataStorage dataStorage, TorrentClient torrentClient) {
        super(dataStorage, torrentClient);
    }

    @Override
    List<SongFileDocument> downloadTorrent(String torrentId,
                                           String torrentName,
                                           List<SongFileDocument> requestedSongs,
                                           TaskProgress taskProgress) {

        final List<SongFileDocument> result = new ArrayList<>();

        final List<SongFileDocument> notDownloadedRequestedSongs = new ArrayList<>(requestedSongs);
        final List<SongFileDocument> alreadyDownloadedRequestedSongs = filterAlreadyExisting2(notDownloadedRequestedSongs);

        final List<SongFileDocument> downloadedRequestedSongs;
        if (!notDownloadedRequestedSongs.isEmpty()) {
            final TorrentDocument torrent = dataStorage.getTorrent(torrentId);

            final List<SongFileDocument> results = new ArrayList<>();

            final File saveDir = new File(new File(AppConfiguration.DOWNLOADED_SONGS_STORAGE), torrentId);
            if (!saveDir.exists()) //noinspection ResultOfMethodCallIgnored
                saveDir.mkdirs();

            boolean allExist = isAllAlreadyDownloaded(saveDir, getCueFiles2(notDownloadedRequestedSongs));
            if (!allExist) {
                // 1. resolve magnet
                taskProgress.log("resolving magnet...");
                final String magnet = torrent.getTorrentInfo().getMagnet();
                logger.info("resoling torrent {} info by magnet {}", torrentId, magnet);
                final TorrentInfo torrentInfo = getByMagnet(magnet);

                logger.info("[{}] magnet resolved, downloading...", torrentName);

                // 2. download
                if (torrentInfo != null) {
                    taskProgress.log("downloading files...");
                    final Priority[] priorities = getAllFilesPriorities(torrentInfo, Lists.transform(notDownloadedRequestedSongs, SongFileDocument::getSongSource));
                    getTorrentClient().download(torrentInfo, saveDir, priorities, taskProgress);
                    logger.info("downloading complete: {}", saveDir.getAbsolutePath());
                }
            }

            // 3. flac -> mp3[]
            taskProgress.log("converting FLAC to MP3...");
            final FlacMp3Converter flacMp3Converter = new FlacMp3Converter();
            for (SongFileDocument notDownloadedRequestedSong : notDownloadedRequestedSongs) {
                final String downloadedCueFile = notDownloadedRequestedSong.getSongSource().getIndexSong().getCueFilePath();

                final File compositionFilesFolder = getCompositionFilesFolder(saveDir, downloadedCueFile);
                //noinspection ConstantConditions
                if (compositionFilesFolder.exists()
                        && compositionFilesFolder.listFiles() != null
                        && compositionFilesFolder.listFiles().length > 0) {
                    continue; // already split
                }

                final String folder = getFolder(downloadedCueFile);
                final String compositionName = getCompositionName(downloadedCueFile);

                final File directory = new File(saveDir, folder);
                flacMp3Converter.convert(directory, compositionName);
            }

            // 4. update resolved sources
            taskProgress.log("saving results...");
            for (SongFileDocument notDownloadedRequestedSong : notDownloadedRequestedSongs) {
                final String downloadedCueFile = notDownloadedRequestedSong.getSongSource().getIndexSong().getCueFilePath();
                final Song song = notDownloadedRequestedSong.getSong();

                // find all sources points to this cue
                final List<SongSourceDocument> cueBasedSources = dataStorage.getSongSourcesByTorrentAndCuePath(torrentId, downloadedCueFile);

                for (SongSourceDocument cueBasedSource : cueBasedSources) {
                    final String cueFilePath = cueBasedSource.getSongSource().getIndexSong().getCueFilePath();
                    final File folder = getCompositionFilesFolder(saveDir, cueFilePath);

                    final String trackNum = cueBasedSource.getSongSource().getIndexSong().getTrackNum();
                    final String songName = cueBasedSource.getSongSource().getIndexSong().getSongName();

                    final File mp3SongFile = new File(folder, String.format("%s. %s.mp3", trackNum, songName));

                    if (mp3SongFile.exists()) {
                        FileDocument fileDocument = createFileDocument(song, cueBasedSource.getSongSource(), mp3SongFile);
                        dataStorage.insertFile(fileDocument);
                        logger.info("[{}: {}] download complete", torrentName, song.getTitle());
                        results.add(new SongFileDocument(song, cueBasedSource.getSongSource(), fileDocument));
                    } else {
                        logger.warn("[{}: {}] download failed", torrentName, song.getTitle());
                    }
                }
            }

            downloadedRequestedSongs = results;
            downloadedRequestedSongs.addAll(alreadyDownloadedRequestedSongs);

        } else {
            downloadedRequestedSongs = alreadyDownloadedRequestedSongs;
        }

        result.addAll(downloadedRequestedSongs);

        return result;
    }

    private String getFolder(String cueFilePath) {
        return cueFilePath.substring(0, cueFilePath.lastIndexOf("\\"));
    }

    private String getCompositionName(String downloadedCueFile) {
        return downloadedCueFile.substring(downloadedCueFile.lastIndexOf("\\") + 1, downloadedCueFile.lastIndexOf("."));
    }

    private File getCompositionFilesFolder(File saveDir, String cueFilePath) {
        final String folderName = getFolder(cueFilePath);
        final String compositionName = getCompositionName(cueFilePath);

        File folder = new File(saveDir, folderName);
        folder = new File(folder, compositionName);
        return folder;
    }

    private Priority[] getAllFilesPriorities(TorrentInfo torrentInfo, List<TorrentSongSource> songSources) {
        final FileStorage files = torrentInfo.files();
        final Priority[] priorities = TorrentUtils.getPrioritiesIgnoreAll(files.numFiles());

        final List<String> filesToDownload = new ArrayList<>();

        for (int i = 0; i < files.numFiles(); i++) {
            final String filePath = files.filePath(i);
            final String fileName = files.fileName(i);
            if (isDownloadFile(filePath, fileName, songSources)) {
                priorities[i] = Priority.NORMAL;
                filesToDownload.add(filePath);
            } else {
                priorities[i] = Priority.IGNORE;
            }
        }

        logger.info("downloading files:");
        for (String file : filesToDownload) {
            logger.info(file);
        }

        return priorities;
    }

    private boolean isDownloadFile(String filePath, String fileName, List<TorrentSongSource> songSources) {
        // todo asm: check
        boolean download = false;
        for (TorrentSongSource songSource : songSources) {
            final String cueFilePath = songSource.getIndexSong().getCueFilePath();
            final int cueFileExtensionPosition = cueFilePath.lastIndexOf(".");
            if (cueFileExtensionPosition < 0) {
                logger.error("bad file {}", cueFilePath);
                continue;
            }
            final int cueFilePathBeginPosition = cueFilePath.startsWith("\\")
                    ? 1
                    : 0;
            final String cueFilePathWithoutExtension = cueFilePath.substring(cueFilePathBeginPosition, cueFileExtensionPosition);

            final int fileExtensionPosition = filePath.lastIndexOf(".");
            if (fileExtensionPosition < 0) {
                // file without extension, skip
                continue;
            }
            final String filePathWithoutExtension = filePath.substring(0, fileExtensionPosition);

            if (filePathWithoutExtension.equals(cueFilePathWithoutExtension)) {
                download = true;
            }
        }

        return download;
    }

    private boolean isAllAlreadyDownloaded(File saveDir, List<String> requestedCues) {
        boolean allExist = true;
        for (String requestedCue : requestedCues) {
            final String folder = getFolder(requestedCue);
            final String compositionName = getCompositionName(requestedCue);

            final File directory = new File(saveDir, folder);
            final File cue = new File(directory, compositionName + ".cue");
            final File flac = new File(directory, compositionName + ".flac");

            if (!cue.exists() || !flac.exists()) {
                allExist = false;
                break;
            }
        }
        return allExist;
    }

    private List<String> getCueFiles2(List<SongFileDocument> songSources) {
        final Set<String> cueFilesPaths = new HashSet<>(Lists.transform(songSources, input -> {
            assert (input != null);
            return input.getSongSource().getIndexSong().getCueFilePath();
        }));
        return new ArrayList<>(cueFilesPaths);
    }
}
