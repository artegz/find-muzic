package ru.asm.core.progress;

/**
 * User: artem.smirnov
 * Date: 12.09.2017
 * Time: 10:29
 */
public class Step {

    private String name;

    public static Step create(String name) {
        final Step step = new Step();
        step.name = name;
        return step;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Step step = (Step) o;

        return name != null ? name.equals(step.name) : step.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
