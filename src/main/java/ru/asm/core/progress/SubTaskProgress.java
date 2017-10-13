package ru.asm.core.progress;

/**
 * User: artem.smirnov
 * Date: 04.10.2017
 * Time: 15:48
 */
class SubTaskProgress {

    private String name;

    private Double progress = null;

    public SubTaskProgress(String name) {
        this.name = name;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public String getName() {
        return name;
    }

    public Double getProgress() {
        return progress;
    }
}
