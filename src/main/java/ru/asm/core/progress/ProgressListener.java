package ru.asm.core.progress;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 8:45
 */
public class ProgressListener {

    public static final String STAGE_RESOLVE = "resolve";
    public static final String STAGE_INDEX = "index";
    public static final String STAGE_DOWNLOAD = "download";

    public static final String SUB_STAGE_DOWNLOAD_FLAC_SOURCE = "downloadFlacSource";
    public static final String SUB_STAGE_DOWNLOAD_MP_3_SOURCE = "downloadMp3Source";
    public static final String SUB_STAGE_INDEX_MP_3_SOURCE = "indexMp3Source";
    public static final String SUB_STAGE_INDEX_FLAC_SOURCE = "indexFlac";


    private Task task;


    ProgressListener(Task task) {
        this.task = task;
    }

    /**
     * Set task current stage
     */
    public void initStage(String stage, List<String> steps, String description) {
        task.setCurrentStage(Stage.create(stage, steps, description));
    }

    /**
     * Specified stage steps are in progress
     */
    public void start(List<String> steps) {
        task.getCurrentStage()
                .setInProgressStageSteps(Lists.transform(steps, Step::create));
    }

    /**
     * Specified stage steps are complete
     */
    public void complete(List<String> steps) {
        task.getCurrentStage()
                .completeStageSteps(Lists.transform(steps, Step::create));
    }

    /**
     * Stage completed
     */
    public void completeStage(String stage) {
        if (!task.getCurrentStage().getName().equals(stage)) {
            throw new IllegalArgumentException();
        }
        task.setCurrentStage(null);
    }

    /**
     * Init sub stage for current in progress steps
     */
    public void initSubStage(String subStage, List<String> subStageSteps, String description) {
        if (task.getCurrentStage() == null) {
            throw new IllegalArgumentException();
        }
        task.getCurrentStage()
                .setCurrentSubStage(Stage.create(subStage, subStageSteps, description));
    }

    public void subStageSkip(List<String> steps) {
        subStageStart(steps);
        subStageComplete(steps);
    }

    public void subStageStart(List<String> steps) {
        task.getCurrentStage()
                .getCurrentSubStage()
                .setInProgressStageSteps(Lists.transform(steps, Step::create));
    }

    public void subStageComplete(List<String> steps) {
        task.getCurrentStage()
                .getCurrentSubStage()
                .completeStageSteps(Lists.transform(steps, Step::create));
    }

    public void completeSubStage(String subStage) {
        if (!task.getCurrentStage().getCurrentSubStage().getName().equals(subStage)) {
            throw new IllegalArgumentException();
        }
        task.getCurrentStage().setCurrentSubStage(null);
    }

    public ProgressBar initSubStageStepProgressBar() {
        return task.getCurrentStage()
                .getCurrentSubStage()
                .initStepProgressBar();
    }

}
