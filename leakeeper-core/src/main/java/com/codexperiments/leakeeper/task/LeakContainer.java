package com.codexperiments.leakeeper.task;

public interface LeakContainer {
    void guard();

    boolean unguard();

    // TODO void rebind(TCallback pTaskResult);
}
