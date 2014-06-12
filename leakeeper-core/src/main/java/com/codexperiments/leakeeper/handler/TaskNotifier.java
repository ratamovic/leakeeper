package com.codexperiments.leakeeper.handler;

/**
 * TODO Pass a progression object.
 */
public interface TaskNotifier<TProgress> {
    void notifyProgress(TProgress pProgress);
}
