package ru.asm.api.baratine;

import io.baratine.service.Result;
import io.baratine.service.Service;
import io.baratine.vault.Asset;
import io.baratine.vault.IdAsset;
import io.baratine.vault.Vault;

import java.util.Collection;

/**
 * User: artem.smirnov
 * Date: 22.08.2017
 * Time: 11:09
 */
@Asset
@Service
public interface TrackVault extends Vault<IdAsset,Track> {

    void create(Track initialData, Result<IdAsset> generatedId);

    void findByTitle(String title, Result<Collection<Track>> result);

    void findAll(Result<Collection<Track>> result);

    void delete(IdAsset id, Result<Boolean> result);

//    private Map<Long,Track> books = new HashMap<Long,Track>();
//
//    public TrackVault()
//    {
//        Track track = new Track();
//
//        track.setId(123);
//        track.setTitle("title0");
//        track.setAuthor("author0");
//
//        books.put(track.getId(), track);
//    }
//
//    @Get("/books")
//    public void getBooks(Result<Collection<Track>> result)
//    {
//        result.ok(books.values());
//    }
//
//    @Get("/books/{id}")
//    public void getBook(@Path("id") Long id, Result<Track> result)
//    {
//        result.ok(books.get(id));
//    }
//
//    @Post("/books")
//    @Modify
//    public void addBook(@Body Track track, Result<Void> result)
//    {
//        books.put(track.getId(), track);
//
//        result.ok(null);
//    }
}
