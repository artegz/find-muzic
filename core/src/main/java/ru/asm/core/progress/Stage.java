package ru.asm.core.progress;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 10:27
 */
public class Stage {

    private String name;

    private String description;

    private List<Step> stageSteps = null;
    private List<Step> inProgressStageSteps = new ArrayList<>();
    private List<Step> completeStageSteps = new ArrayList<>();

    private Stage subStage = null;

    private ProgressBar progressBar = null;

    public static Stage create(String stageName,
                               List<String> stepNames,
                               String stageDescription) {
        final Stage stage = new Stage();
        stage.name = stageName;
        stage.description = stageDescription;
        stage.stageSteps = Lists.transform(stepNames, Step::create);
        return stage;
    }

    public List<Step> getStageSteps() {
        return stageSteps;
    }

    public List<Step> getInProgressStageSteps() {
        return inProgressStageSteps;
    }

    public List<Step> getCompleteStageSteps() {
        return completeStageSteps;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Stage getSubStage() {
        return subStage;
    }

    public void setInProgressStageSteps(List<Step> inProgressStageSteps) {
        this.inProgressStageSteps.addAll(inProgressStageSteps);
    }

    public void completeStageSteps(List<Step> steps) {
        this.inProgressStageSteps.removeAll(steps);
        this.completeStageSteps.addAll(steps);
    }

    public void setCurrentSubStage(Stage subStage) {
        this.subStage = subStage;
    }

    public Stage getCurrentSubStage() {
        return subStage;
    }

    public ProgressBar initStepProgressBar() {
        this.progressBar = new ProgressBar();
        return this.progressBar;
    }
}
