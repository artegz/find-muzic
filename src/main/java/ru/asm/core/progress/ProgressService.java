package ru.asm.core.progress;

import org.springframework.stereotype.Component;
import ru.asm.core.dev.model.Artist;
import ru.asm.core.dev.model.Song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 8:51
 */
@Component
public class ProgressService {

    private Map<String, TaskProgress> tasksInProgress = new HashMap<>();
    private List<String> queuedTasks = new ArrayList<>();

    public static String downloadArtistSongsTaskId(Integer artistId) {
        return String.valueOf(artistId);
    }

    public static String downloadTorrentsTaskId(String torrentId) {
        return String.valueOf(torrentId);
    }

    public static String resolveArtistSongsTaskId(Integer artistId) {
        return String.valueOf(artistId);
    }

    public static String searchSongTaskId(Integer songId) {
        return String.valueOf(songId);
    }

    public static String indexArtistTaskId(Integer artistId) {
        return String.valueOf(artistId);
    }

    public boolean isInProgress() {
        return !tasksInProgress.isEmpty();
    }

    public void queueTasks(List<String> taskIds, String taskType) {
        for (String taskId : taskIds) {
            queuedTasks.add(asKey(taskType, taskId));
        }
    }

    public TaskProgress taskStarted(String taskId, String taskName, Artist artist, List<Song> artistSongs, String taskType) {
        final String key = asKey(taskType, taskId);

        final TaskProgress taskProgress = new TaskProgress(taskId, taskName, taskType, artist, artistSongs);
        tasksInProgress.put(key, taskProgress);
        queuedTasks.remove(key);
        return taskProgress;
    }

    public void taskEnded(String taskId, String taskType) {
        final String key = asKey(taskType, taskId);
        tasksInProgress.remove(key);
        queuedTasks.remove(key);
    }

    @Deprecated
    public Map<Integer, Task> getTasksInProgress() {
        return new HashMap<>();
    }

    public int getNumTasksInProgress() {
        return tasksInProgress.keySet().size();
    }
    public int getNumQueuedTasks() {
        return queuedTasks.size();
    }

    public List<TaskProgress> getTasksInProgress2() {
        return new ArrayList<>(tasksInProgress.values());
    }

    private static String asKey(String taskType, String taskId) {
        return String.format("%s_%s", taskId, taskType);
    }
}
