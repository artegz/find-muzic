package ru.asm.core.index.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * User: artem.smirnov
 * Date: 07.06.2016
 * Time: 9:01
 */
@Document(indexName = "torrentinfo")
public class TorrentInfoVO {

    @Id
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String id;

    @Field(type = FieldType.String)
    private String title;

    @Field(type = FieldType.Long, index = FieldIndex.no)
    private Long size;

    @Field(type = FieldType.String, index = FieldIndex.no)
    private String magnet;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String forumId;

    @Field(type = FieldType.String)
    private String forum;

    @Field(type = FieldType.Date)
    private Date creationDate;

    public TorrentInfoVO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setForumId(String forumId) {
        this.forumId = forumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getMagnet() {
        return magnet;
    }

    public void setMagnet(String magnet) {
        this.magnet = magnet;
    }

    public String getForumId() {
        return forumId;
    }

    public String getForum() {
        return forum;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TorrentInfoVO{");
        sb.append("id='").append(id).append('\'');
        sb.append(", title='").append(title).append('\'');
        sb.append(", size=").append(size);
        sb.append(", magnet='").append(magnet).append('\'');
        sb.append(", forum='").append(forum).append('\'');
        sb.append(", creationDate=").append(creationDate);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TorrentInfoVO that = (TorrentInfoVO) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (size != null ? !size.equals(that.size) : that.size != null) return false;
        if (magnet != null ? !magnet.equals(that.magnet) : that.magnet != null) return false;
        if (forum != null ? !forum.equals(that.forum) : that.forum != null) return false;
        return creationDate != null ? creationDate.equals(that.creationDate) : that.creationDate == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (magnet != null ? magnet.hashCode() : 0);
        result = 31 * result + (forum != null ? forum.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        return result;
    }
}
