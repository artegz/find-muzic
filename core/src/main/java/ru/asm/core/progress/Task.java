package ru.asm.core.progress;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 10:27
 */
public class Task {

    private String name;

    private Stage currentStage = null;

    public Task(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

}
