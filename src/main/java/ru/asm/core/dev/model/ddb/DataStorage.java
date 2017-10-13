package ru.asm.core.dev.model.ddb;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.UpdateOptions;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.springframework.stereotype.Component;
import ru.asm.core.AppConfiguration;
import ru.asm.core.dev.model.ArtistResolveReport;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 30.08.2017
 * Time: 15:18
 */
@Component
public class DataStorage {

    private Nitrite db;

    private ObjectRepository<ArtistDocument> artistRepo;
    private ObjectRepository<TorrentDocument> torrentRepo;
//    private ObjectRepository<SongDocument> songRepo;
    private ObjectRepository<FileDocument> filesRepo;
    private ObjectRepository<SongSourceDocument> songSourceRepo;

    private ObjectRepository<ArtistResolveReport> songResolveReportRepo;

    @PostConstruct
    public void postConstruct() {
        //java initialization
        final File folder = new File(AppConfiguration.N2O_DB_FILE_LOCATION);
        if (!folder.exists()) folder.mkdirs();
        db = Nitrite.builder()
                .filePath(new File(folder, "test.db"))
                .openOrCreate("user", "password");
        artistRepo = db.getRepository(ArtistDocument.class);
        torrentRepo = db.getRepository(TorrentDocument.class);
//        songRepo = db.getRepository(SongDocument.class);
        filesRepo = db.getRepository(FileDocument.class);
        songSourceRepo = db.getRepository(SongSourceDocument.class);
        songResolveReportRepo = db.getRepository(ArtistResolveReport.class);
    }

    public ArtistResolveReport getSongResolveReport(Integer artistId) {
        final Cursor<ArtistResolveReport> cursor = songResolveReportRepo.find(ObjectFilters.eq("artistId", artistId));
        return cursor.firstOrDefault();
    }

    public void saveArtistResolveReport(ArtistResolveReport report) {
        final UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.setUpsert(true);
        songResolveReportRepo.update(ObjectFilters.eq("artistId", report.getArtistId()), report, updateOptions);
    }


    public ArtistDocument getArtist(Integer artistId) {
        final Cursor<ArtistDocument> artistDocuments = artistRepo.find(ObjectFilters.eq("artistId", artistId));
        return artistDocuments.firstOrDefault();
    }

    public void updateArtist(ArtistDocument artist) {
        final UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.setUpsert(true);
        artistRepo.update(ObjectFilters.eq("artistId", artist.getArtistId()), artist, updateOptions);
    }

    public TorrentDocument getTorrent(String torrentId) {
        final Cursor<TorrentDocument> artistDocuments = torrentRepo.find(ObjectFilters.eq("torrentId", torrentId));
        return artistDocuments.firstOrDefault();
    }

    public void updateTorrent(TorrentDocument torrentDocument) {
        final UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.setUpsert(true);
        torrentRepo.update(ObjectFilters.eq("torrentId", torrentDocument.getTorrentId()), torrentDocument, updateOptions);
    }

    public FileDocument getFileBySource(String sourceId) {
        final Cursor<FileDocument> fileDocuments = filesRepo.find(ObjectFilters.eq("sourceId", sourceId));
        return fileDocuments.firstOrDefault();
    }

    public List<FileDocument> getFiles(Integer songId) {
        final Cursor<FileDocument> fileDocuments = filesRepo.find(ObjectFilters.eq("songId", songId));
        return fileDocuments.toList();
    }

    public void insertFile(FileDocument fileDocument) {
        filesRepo.insert(fileDocument);
    }

    public List<SongSourceDocument> getSongSources(Integer songId) {
        final Cursor<SongSourceDocument> songSourceDocuments = songSourceRepo.find(ObjectFilters.eq("songId", songId));
        return songSourceDocuments.toList();
    }

    public List<SongSourceDocument> getSongSourcesByTorrent(String torrentId) {
        final Cursor<SongSourceDocument> songSourceDocuments = songSourceRepo.find(ObjectFilters.eq("songSource.indexSong.torrentId", torrentId));
        return songSourceDocuments.toList();
    }

    public List<SongSourceDocument> getSongSourcesByTorrentAndCuePath(String torrentId, String cuePath) {
        final Cursor<SongSourceDocument> songSourceDocuments = songSourceRepo.find(
                ObjectFilters.and(
                        ObjectFilters.eq("songSource.indexSong.torrentId", torrentId),
                        ObjectFilters.eq("songSource.indexSong.cueFilePath", cuePath)
                )
        );
        return songSourceDocuments.toList();
    }

    public SongSourceDocument getSongSource(String sourceId) {
        final Cursor<SongSourceDocument> songSourceDocuments = songSourceRepo.find(ObjectFilters.eq("sourceId", sourceId));
        return songSourceDocuments.firstOrDefault();
    }

    public void updateSongSource(SongSourceDocument songSourceDocument) {
        final UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.setUpsert(true);
        songSourceRepo.update(ObjectFilters.eq("sourceId", songSourceDocument.getSourceId()), songSourceDocument, updateOptions);
    }
}
