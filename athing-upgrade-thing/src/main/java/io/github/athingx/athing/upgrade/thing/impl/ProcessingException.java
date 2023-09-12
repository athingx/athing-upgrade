package io.github.athingx.athing.upgrade.thing.impl;

public class ProcessingException extends RuntimeException {

    private final int step;

    public ProcessingException(int step, String message) {
        super(message);
        this.step = step;
    }

    public ProcessingException(int step, String message, Throwable cause) {
        super(message, cause);
        this.step = step;
    }

    public int getStep() {
        return step;
    }

}
