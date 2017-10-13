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

    public static final String STAGE_RESOLVE = "resolve";
    public static final String STAGE_INDEX = "index";
    public static final String STAGE_DOWNLOAD = "download";

    public static final String SUB_STAGE_DOWNLOAD_FLAC_SOURCE = "downloadFlacSource";
    public static final String SUB_STAGE_DOWNLOAD_MP_3_SOURCE = "downloadMp3Source";
    public static final String SUB_STAGE_INDEX_MP_3_SOURCE = "indexMp3Source";
    public static final String SUB_STAGE_INDEX_FLAC_SOURCE = "indexFlac";


    //    private Task task;


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

    /**
     * Set task current stage
     */
//    @Deprecated
//    public void initStage(String stage, List<String> steps, String description) {
//        task.setCurrentStage(Stage.create(stage, steps, description));
//    }

    /**
     * Specified stage steps are in progress
     */
//    @Deprecated
//    public void start(List<String> steps) {
//        task.getCurrentStage()
//                .setInProgressStageSteps(Lists.transform(steps, Step::create));
//    }

    /**
     * Specified stage steps are complete
     */
//    @Deprecated
//    public void complete(List<String> steps) {
//        task.getCurrentStage()
//                .completeStageSteps(Lists.transform(steps, Step::create));
//    }

    /**
     * Stage completed
     */
//    @Deprecated
//    public void completeStage(String stage) {
//        if (!task.getCurrentStage().getName().equals(stage)) {
//            throw new IllegalArgumentException();
//        }
//        task.setCurrentStage(null);
//    }

    /**
     * Init sub stage for current in progress steps
     */
//    @Deprecated
//    public void initSubStage(String subStage, List<String> subStageSteps, String description) {
//        if (task.getCurrentStage() == null) {
//            throw new IllegalArgumentException();
//        }
//        task.getCurrentStage()
//                .setCurrentSubStage(Stage.create(subStage, subStageSteps, description));
//    }

//    @Deprecated
//    public void subStageSkip(List<String> steps) {
//        subStageStart(steps);
//        subStageComplete(steps);
//    }

//    @Deprecated
//    public void subStageStart(List<String> steps) {
//        task.getCurrentStage()
//                .getCurrentSubStage()
//                .setInProgressStageSteps(Lists.transform(steps, Step::create));
//    }

//    @Deprecated
//    public void subStageComplete(List<String> steps) {
//        task.getCurrentStage()
//                .getCurrentSubStage()
//                .completeStageSteps(Lists.transform(steps, Step::create));
//    }

//    @Deprecated
//    public void completeSubStage(String subStage) {
//        if (!task.getCurrentStage().getCurrentSubStage().getName().equals(subStage)) {
//            throw new IllegalArgumentException();
//        }
//        task.getCurrentStage().setCurrentSubStage(null);
//    }

//    @Deprecated
//    public ProgressBar initSubStageStepProgressBar() {
//        return task.getCurrentStage()
//                .getCurrentSubStage()
//                .initStepProgressBar();
//    }

}
