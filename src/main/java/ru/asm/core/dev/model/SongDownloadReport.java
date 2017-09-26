package ru.asm.core.dev.model;

import java.util.Date;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 13.09.2017
 * Time: 9:18
 */
public class SongDownloadReport {

    private Integer songId;
    private Integer artistId;

    private Date startTime;
    private Date endTime;

    private Map<String, String> sourcesDownloadStatuses = null;
}
