package com.codexperiments.leakeeper.task;

public interface TaskScheduler {
    void schedule(Runnable pRunnable);

    void scheduleIfNecessary(Runnable pRunnable);
}