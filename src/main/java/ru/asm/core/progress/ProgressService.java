package ru.asm.core.progress;

import org.springframework.stereotype.Component;
import ru.asm.core.dev.model.Song;

import java.util.HashMap;
import java.util.Map;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 8:51
 */
@Component
public class ProgressService {

    private Map<Integer, Task> tasksInProgress = new HashMap<>();

    public boolean isInProgress() {
        return !tasksInProgress.isEmpty();
    }

    public ProgressListener taskStarted(Song song, String taskName) {
        final Task newTask = new Task(taskName);
        tasksInProgress.put(song.getSongId(), newTask);
        return new ProgressListener(newTask);
    }

    public void taskEnded(Song song) {
        tasksInProgress.remove(song.getSongId());
    }

    public Map<Integer, Task> getTasksInProgress() {
        return tasksInProgress;
    }
}
