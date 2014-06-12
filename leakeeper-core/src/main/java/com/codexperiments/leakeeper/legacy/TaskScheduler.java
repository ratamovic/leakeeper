package com.codexperiments.leakeeper.legacy;

public interface TaskScheduler {
    void schedule(Runnable pRunnable);

    void scheduleIfNecessary(Runnable pRunnable);
}