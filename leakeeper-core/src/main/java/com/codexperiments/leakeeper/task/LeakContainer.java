package com.codexperiments.leakeeper.task;

public interface LeakContainer {
    void guard();

    boolean unguard();
}
