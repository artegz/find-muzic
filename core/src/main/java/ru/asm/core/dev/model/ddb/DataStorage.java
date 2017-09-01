package ru.asm.core.dev.model.ddb;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void postConstruct() {
        //java initialization
        db = Nitrite.builder()
                .filePath("/tmp/test.db")
                .openOrCreate("user", "password");
        artistRepo = db.getRepository(ArtistDocument.class);
        torrentRepo = db.getRepository(TorrentDocument.class);
//        songRepo = db.getRepository(SongDocument.class);
        filesRepo = db.getRepository(FileDocument.class);
        songSourceRepo = db.getRepository(SongSourceDocument.class);
    }

    public ArtistDocument getArtist(Integer artistId) {
        final Cursor<ArtistDocument> artistDocuments = artistRepo.find(ObjectFilters.eq("artistId", artistId));
        return artistDocuments.firstOrDefault();
    }

    public void insertArtist(ArtistDocument artist) {
        artistRepo.insert(artist);
    }

    public void updateArtist(ArtistDocument artist) {
        artistRepo.update(artist);
    }

    public TorrentDocument getTorrent(String torrentId) {
        final Cursor<TorrentDocument> artistDocuments = torrentRepo.find(ObjectFilters.eq("torrentId", torrentId));
        return artistDocuments.firstOrDefault();
    }

    public void insertTorrent(TorrentDocument torrentDocument) {
        torrentRepo.insert(torrentDocument);
    }

    public void updateTorrent(TorrentDocument torrentDocument) {
        torrentRepo.update(torrentDocument);
    }

//    public SongDocument getSong(Integer songId) {
//        final Cursor<SongDocument> songDocuments = songRepo.find(ObjectFilters.eq("songId", songId));
//        return songDocuments.firstOrDefault();
//    }
//
//    public void insertSong(SongDocument songDocument) {
//        songRepo.insert(songDocument);
//    }
//
//    public void updateSong(SongDocument songDocument) {
//        songRepo.update(ObjectFilters.eq("songId", songDocument.getSongId()), songDocument);
//    }

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

    public SongSourceDocument getSongSource(String sourceId) {
        final Cursor<SongSourceDocument> songSourceDocuments = songSourceRepo.find(ObjectFilters.eq("sourceId", sourceId));
        return songSourceDocuments.firstOrDefault();
    }

    public void insertSongSource(SongSourceDocument songSourceDocument) {
        songSourceRepo.insert(songSourceDocument);
    }
}
