package ru.asm.core.progress;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 9:29
 */
public class ProgressBar {

    private Double progress = null;

    private boolean completed = false;

    public void reset() {
        progress = 0d;
    }

    public void complete() {
        this.progress = null;
        this.completed = true;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }
}
