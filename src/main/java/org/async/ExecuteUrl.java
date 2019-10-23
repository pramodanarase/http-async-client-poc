package org.async;

import java.util.concurrent.Callable;

public interface ExecuteUrl extends Callable<Object> {
    void setDone(boolean done);
    boolean isDone();
    long getId();
}
