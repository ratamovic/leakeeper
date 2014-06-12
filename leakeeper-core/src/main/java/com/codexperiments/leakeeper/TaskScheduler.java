package com.codexperiments.leakeeper;

public interface TaskScheduler {
    void schedule(Runnable pRunnable);

    void scheduleIfNecessary(Runnable pRunnable);
}