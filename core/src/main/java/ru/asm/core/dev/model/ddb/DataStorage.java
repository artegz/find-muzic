package ru.asm.core.dev.model.ddb;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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

    @PostConstruct
    public void postConstruct() {
        //java initialization
        db = Nitrite.builder()
                .filePath("/tmp/test.db")
                .openOrCreate("user", "password");
        artistRepo = db.getRepository(ArtistDocument.class);
        torrentRepo = db.getRepository(TorrentDocument.class);
    }

    public ArtistDocument getArtist(Integer artistId) {
        final Cursor<ArtistDocument> artistDocuments = artistRepo.find(ObjectFilters.eq("artistId", artistId));
        return artistDocuments.firstOrDefault();
    }

    public void saveArtist(ArtistDocument artist) {
        artistRepo.insert(artist);
    }

    public TorrentDocument getTorrent(String torrentId) {
        final Cursor<TorrentDocument> artistDocuments = torrentRepo.find(ObjectFilters.eq("torrentId", torrentId));
        return artistDocuments.firstOrDefault();
    }

    public void insertTorrent(TorrentDocument torrentDocument) {
        torrentRepo.insert(torrentDocument);
    }

    public void updateTorrent(TorrentDocument torrentDocument) {
        torrentRepo.update(ObjectFilters.eq("torrentId", torrentDocument.getTorrentId()), torrentDocument);
    }
}
