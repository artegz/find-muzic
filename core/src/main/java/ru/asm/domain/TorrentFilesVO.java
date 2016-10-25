package ru.asm.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
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
    @Field(type = FieldType.String)
    private String id;

    @Field(type = FieldType.Auto)
    private List<String> fileNames;

}
