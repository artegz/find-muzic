package ru.asm.core;

import com.frostwire.jlibtorrent.TorrentInfo;
import org.apache.commons.lang.mutable.MutableLong;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.torrent.TorrentClient;
import ru.asm.core.ttdb.TorrentsDbParser;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 28.08.2017
 * Time: 10:53
 */
@Component
public class AppCoreService {

    private static final Logger logger = LoggerFactory.getLogger(AppCoreService.class);

    private static final int GROUP_SIZE = 10000;


    @Autowired
    private Node node;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private TorrentInfoRepository torrentInfoRepository;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private PlaylistSongsMapper playlistSongsMapper;


    private TorrentClient torrentClient;


    @PostConstruct
    public void postConstruct() {
        logger.info("initializing...");

        // await at least for yellow status
        final ClusterHealthResponse response = elasticsearchOperations.getClient()
                .admin()
                .cluster()
                .prepareHealth()
                .setWaitForGreenStatus()
                .get();
        if (response.getStatus() != ClusterHealthStatus.YELLOW) {
            throw new IllegalStateException("repository is not initialized");
        }
        logger.info("elasticsearch initialized");

        torrentClient = new TorrentClient();
        torrentClient.initializeSession();
        logger.info("torrent client initialized");

        logger.info("initialization complete");
    }

    @PreDestroy
    public void preDestroy() {
        logger.info("destroying...");
        node.close();
        torrentClient.destroySession();
        logger.info("destroying complete");
    }

    public TorrentClient getTorrentClient() {
        return torrentClient;
    }

    public void downloadTorrent(File folder, String magnet) {
        if (!folder.exists()) //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();

        try {
            TorrentInfo torrentInfo = getByMagnet(magnet);

            String name = torrentInfo.name();
            File resumeFile = new File(folder, name + ".tmp");
            if (!resumeFile.exists()) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    resumeFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                }
            }

            torrentClient.download(torrentInfo, folder, null, null);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            logger.debug("download done");
        }
    }

    @Transactional
    public void importPlaylist(String playlistName, String comment, File playlist) {
        List<SongDescriptor> songs = FileTools.readCsv(playlist);

        int i = 1;
        for (SongDescriptor song : songs) {
            final String artist = song.getArtist();
            final String songTitle = song.getTitle();

            if (artist != null && !artist.isEmpty() && songTitle != null) {
                Integer artistId = playlistSongsMapper.findArtistIdByName(artist);
                if (artistId == null) {
                    playlistSongsMapper.insertArtist(null, artist);
                    artistId = playlistSongsMapper.findArtistIdByName(artist);
                }
                assert (artistId != null);

                Integer songId = playlistSongsMapper.findSongIdByNameAndArtist(artistId, songTitle);
                if (songId == null) {
                    playlistSongsMapper.insertSong(null, artistId, songTitle);
                    songId = playlistSongsMapper.findSongIdByNameAndArtist(artistId, songTitle);
                }
                assert (songId != null);


                playlistSongsMapper.insertPlaylistSong(artistId, songId, playlistName, comment, i);
                logger.info("{}. [{}] '{}' into '{}' with comment: '{}'", i++, song.getArtist(), song.getTitle(), playlistName, comment);
            } else {
                logger.warn("{}. null", i++);
            }
        }
    }

    public void indexTorrentsDb(File backup) {
        final InputStream inputStream;
        try {
            inputStream = new FileInputStream(backup);
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            return;
        }

        try {
            MutableLong totalIndexed = new MutableLong(0);
            Long count = torrentInfoRepository.count();
            logInfo("%d entries in repository", count);

            try {
                logInfo("start reading backup", count);
                TorrentsDbParser.parseDocument(inputStream, GROUP_SIZE, torrentInfos -> {
                    logInfo("%s more entries read", torrentInfos.size());
                    torrentInfoRepository.save(torrentInfos);
                    totalIndexed.add(torrentInfos.size());
                    logInfo("%d entries added into index (total: %d)", torrentInfos.size(), totalIndexed.longValue());
                });
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }

            count = torrentInfoRepository.count();
            logInfo("%d entries in repository", count);
        } finally {
            logInfo("closing opened resources");
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private TorrentInfo getByMagnet(String magnet) {
        return torrentClient.findByMagnet(magnet);
    }

    private static void logInfo(String message, Object... args) {
        if (message != null) {
            logger.info(String.format(message, args));
        }
    }

}
