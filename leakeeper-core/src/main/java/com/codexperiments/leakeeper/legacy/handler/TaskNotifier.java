package com.codexperiments.leakeeper.legacy.handler;

/**
 * TODO Pass a progression object.
 */
public interface TaskNotifier<TProgress> {
    void notifyProgress(TProgress pProgress);
}
