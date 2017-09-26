package ru.asm.api.baratine;

import io.baratine.service.Modify;
import io.baratine.service.Result;
import io.baratine.vault.Asset;
import io.baratine.vault.Id;
import io.baratine.vault.IdAsset;

/**
 * User: artem.smirnov
 * Date: 22.08.2017
 * Time: 10:50
 */
@Asset
public class Track {

    @Id
    private IdAsset id;

    private String title;
    private String author;

    public void get(Result<Track> result) {
        result.okShim(this);
    }

    @Modify
    public void set(Track track, Result<Boolean> result) {
        this.title = track.title;
        this.author = track.author;

        result.ok(true);
    }

    public IdAsset getId() {
        return id;
    }

    public void setId(IdAsset id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
