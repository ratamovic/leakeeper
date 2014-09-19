package com.codexperiments.leakeeper.task;

import com.codexperiments.leakeeper.task.LeakManagerConfig;
import com.codexperiments.leakeeper.task.impl.LeakManagerImpl;

/**
 * Allows changing the way things are synchronized in the code (i.e. everything on the UI-Thread or multi-threaded).
 */
public interface LeakManagerFactory {
    <TCallback> LeakManagerImpl<TCallback> createManager(Class<TCallback> pCallbackClass, LeakManagerConfig pConfig);
}