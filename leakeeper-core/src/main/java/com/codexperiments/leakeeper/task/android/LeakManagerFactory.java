package com.codexperiments.leakeeper.task.android;

import com.codexperiments.leakeeper.task.LeakManagerConfig;

import java.util.concurrent.locks.Lock;

/**
 * Allows changing the way things are synchronized in the code (i.e. everything on the UI-Thread or multi-threaded).
 */
public interface LeakManagerFactory {
    <TCallback> AndroidLeakManager<TCallback> createManager(Class<TCallback> pCallbackClass, LeakManagerConfig pConfig);
}