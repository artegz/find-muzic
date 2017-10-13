package ru.asm.core.progress;

import java.util.List;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 11:22
 */
public class SimpleProgressInfo {

    private int numQueued;

    private List<TaskProgress> taskProgresses;

    public SimpleProgressInfo(int numQueued, List<TaskProgress> taskProgresses) {
        this.numQueued = numQueued;
        this.taskProgresses = taskProgresses;
    }

    public int getNumQueued() {
        return numQueued;
    }

    public List<TaskProgress> getTaskProgresses() {
        return taskProgresses;
    }
}
