package ru.asm.core.progress;

import ru.asm.core.dev.model.Artist;
import ru.asm.core.dev.model.Song;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 8:45
 */
public class TaskProgress {

    private final String taskId;
    private final String taskName;
    private final String taskType;
    private final Artist artist;
    private final List<Song> artistSongs;

    private Map<Long, SubTaskProgress> subTasks = new HashMap<>();
    private String lastMessage;
    private Double progress = null;


    public TaskProgress() {
        this.taskId = "n/a";
        this.taskName = "n/a";
        this.artist = null;
        this.artistSongs = null;
        this.taskType = "n/a";
    }

    TaskProgress(String taskId, String taskName, String taskType, Artist artist, List<Song> artistSongs) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.artist = artist;
        this.artistSongs = artistSongs;
    }

    public long startSubTask(String name) {
        final long tId = System.nanoTime();
        subTasks.put(tId, new SubTaskProgress(name));
        lastMessage = null;
        return tId;
    }
    public void setSubTaskProgress(long tId, Double absProgress) {
        if (subTasks.containsKey(tId)) {
            subTasks.get(tId).setProgress(absProgress);
        } else {
            throw new IllegalArgumentException("task not found");
        }
    }
    public void setSubTaskProgress(long tId, int done, int total) {
        setSubTaskProgress(tId, ((double) done) / ((double) total));
    }
    public void completeSubTask(long tId) {
        subTasks.remove(tId);
        lastMessage = null;
    }

    public void log(String message) {
        this.lastMessage = message;
    }

    public void log(String message, Object... params) {
        this.lastMessage = String.format(message, params);
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public Artist getArtist() {
        return artist;
    }

    public List<Song> getArtistSongs() {
        return artistSongs;
    }

    public Map<Long, SubTaskProgress> getSubTasks() {
        return subTasks;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

}
