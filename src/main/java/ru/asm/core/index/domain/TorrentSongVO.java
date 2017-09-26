package ru.asm.core.index.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * User: artem.smirnov
 * Date: 24.03.2017
 * Time: 17:15
 */
@Document(indexName = "torrentsong")
public class TorrentSongVO {

    @Id
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String id;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String torrentId;


    @Field(type = FieldType.String)
    private String artistName;

    @Field(type = FieldType.String)
    private String songName;

    @Field(type = FieldType.String)
    private String type; //flac or mp3


    // MP3

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String mp3FileName;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String mp3FilePath;


    // flac

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String cueFileName;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String cueFilePath;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String trackNum;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String indexTime;

    public TorrentSongVO() {
    }

    public TorrentSongVO(String torrentId, String songName, String artistName, String mp3FileName, String mp3FilePath) {
        this.songName = songName;
        this.artistName = artistName;
        this.mp3FileName = mp3FileName;
        this.mp3FilePath = mp3FilePath;
        this.type = "MP3";
        this.torrentId = torrentId;
        this.id = String.format("%s_%s_%s", torrentId, mp3FilePath.hashCode(), mp3FileName);
    }

    public TorrentSongVO(String torrentId, String songName, String artistName, String cueFileName, String cueFilePath, String trackNum, String indexTime) {
        this.songName = songName;
        this.artistName = artistName;
        this.cueFileName = cueFileName;
        this.cueFilePath = cueFilePath;
        this.indexTime = indexTime;
        this.trackNum = trackNum;
        this.type = "FLAC";
        this.torrentId = torrentId;
        this.id = String.format("%s_%s_%s#%s", torrentId, cueFilePath.hashCode(), cueFilePath, trackNum);
    }

    public String getArtistName() {
        return artistName;
    }

    public String getTorrentId() {
        return torrentId;
    }

    public String getId() {
        return id;
    }

    public String getTrackNum() {
        return trackNum;
    }

    public String getSongName() {
        return songName;
    }

    public String getType() {
        return type;
    }

    public String getMp3FileName() {
        return mp3FileName;
    }

    public String getMp3FilePath() {
        return mp3FilePath;
    }

    public String getCueFileName() {
        return cueFileName;
    }

    public String getCueFilePath() {
        return cueFilePath;
    }

    public String getIndexTime() {
        return indexTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TorrentSongVO that = (TorrentSongVO) o;

        if (songName != null ? !songName.equals(that.songName) : that.songName != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (mp3FileName != null ? !mp3FileName.equals(that.mp3FileName) : that.mp3FileName != null) return false;
        if (mp3FilePath != null ? !mp3FilePath.equals(that.mp3FilePath) : that.mp3FilePath != null) return false;
        if (cueFileName != null ? !cueFileName.equals(that.cueFileName) : that.cueFileName != null) return false;
        if (cueFilePath != null ? !cueFilePath.equals(that.cueFilePath) : that.cueFilePath != null) return false;
        return indexTime != null ? indexTime.equals(that.indexTime) : that.indexTime == null;
    }

    @Override
    public int hashCode() {
        int result = songName != null ? songName.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (mp3FileName != null ? mp3FileName.hashCode() : 0);
        result = 31 * result + (mp3FilePath != null ? mp3FilePath.hashCode() : 0);
        result = 31 * result + (cueFileName != null ? cueFileName.hashCode() : 0);
        result = 31 * result + (cueFilePath != null ? cueFilePath.hashCode() : 0);
        result = 31 * result + (indexTime != null ? indexTime.hashCode() : 0);
        return result;
    }
}
