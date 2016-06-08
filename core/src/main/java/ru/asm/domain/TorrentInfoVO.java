package ru.asm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
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
    @Field(type = FieldType.String)
    private String id;

    @Field(type = FieldType.String)
    private String title;

    @Field(type = FieldType.Long)
    private Long size;

    @Field(type = FieldType.Integer)
    private Integer seedsNum;

    @Field(type = FieldType.Integer)
    private Integer peerNum;

    @Field(type = FieldType.String)
    private String hash;

    @Field(type = FieldType.Integer)
    private Integer downloads;

    @Field(type = FieldType.Date)
    private Date creationDate;

    @Field(type = FieldType.String)
    private String category; // with sub categories separated with |

    public TorrentInfoVO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Integer getSeedsNum() {
        return seedsNum;
    }

    public void setSeedsNum(Integer seedsNum) {
        this.seedsNum = seedsNum;
    }

    public Integer getPeerNum() {
        return peerNum;
    }

    public void setPeerNum(Integer peerNum) {
        this.peerNum = peerNum;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Integer getDownloads() {
        return downloads;
    }

    public void setDownloads(Integer downloads) {
        this.downloads = downloads;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TorrentInfoVO{");
        sb.append("id='").append(id).append('\'');
        sb.append(", title='").append(title).append('\'');
        sb.append(", size=").append(size);
        sb.append(", seedsNum=").append(seedsNum);
        sb.append(", peerNum=").append(peerNum);
        sb.append(", hash='").append(hash).append('\'');
        sb.append(", downloads=").append(downloads);
        sb.append(", creationDate=").append(creationDate);
        sb.append(", category='").append(category).append('\'');
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
        if (seedsNum != null ? !seedsNum.equals(that.seedsNum) : that.seedsNum != null) return false;
        if (peerNum != null ? !peerNum.equals(that.peerNum) : that.peerNum != null) return false;
        if (hash != null ? !hash.equals(that.hash) : that.hash != null) return false;
        if (downloads != null ? !downloads.equals(that.downloads) : that.downloads != null) return false;
        if (creationDate != null ? !creationDate.equals(that.creationDate) : that.creationDate != null) return false;
        return category != null ? category.equals(that.category) : that.category == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (seedsNum != null ? seedsNum.hashCode() : 0);
        result = 31 * result + (peerNum != null ? peerNum.hashCode() : 0);
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        result = 31 * result + (downloads != null ? downloads.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (category != null ? category.hashCode() : 0);
        return result;
    }
}
