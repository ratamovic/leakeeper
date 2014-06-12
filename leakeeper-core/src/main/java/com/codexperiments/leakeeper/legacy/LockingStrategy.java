package com.codexperiments.leakeeper.legacy;

import java.util.concurrent.locks.Lock;

/**
 * Allows changing the way things are synchronized in the code (i.e. everything on the UI-Thread or multi-threaded).
 */
public interface LockingStrategy {
    void createManager(CallbackManager pAndroidTaskManager);

    Lock createLock();

    void checkCallIsAllowed();
}
