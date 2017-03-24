package ru.asm.core.index.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 16.06.2016
 * Time: 16:53
 */
@Document(indexName = "torrentfiles")
public class TorrentFilesVO {

    @Id
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String torrentId;

    @Field(type = FieldType.Integer, index = FieldIndex.not_analyzed)
    private Integer artistId;

    @Field(type = FieldType.String)
    private String artist;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String forumId;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String magnet;


    @Field(type = FieldType.Auto)
    private List<String> fileNames;

    public String getTorrentId() {
        return torrentId;
    }

    public void setTorrentId(String torrentId) {
        this.torrentId = torrentId;
    }

    public Integer getArtistId() {
        return artistId;
    }

    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getForumId() {
        return forumId;
    }

    public void setForumId(String forumId) {
        this.forumId = forumId;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public String getMagnet() {
        return magnet;
    }

    public void setMagnet(String magnet) {
        this.magnet = magnet;
    }
}
