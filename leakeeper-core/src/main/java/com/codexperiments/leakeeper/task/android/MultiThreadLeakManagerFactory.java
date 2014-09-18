package com.codexperiments.leakeeper.task.android;

import android.os.Looper;
import com.codexperiments.leakeeper.task.LeakContainer;
import com.codexperiments.leakeeper.task.LeakManagerConfig;
import com.codexperiments.leakeeper.task.util.AutoCleanMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.codexperiments.leakeeper.task.android.AndroidLeakManagerException.mustBeExecutedFromUIThread;

/**
 * Tasks and handlers can be executed on any threads concurrently.
 */
public class MultiThreadLeakManagerFactory implements LeakManagerFactory {
    private static final int DEFAULT_CAPACITY = 64;

    @Override
    public <TCallback> AndroidLeakManager<TCallback> createManager(Class<TCallback> pCallbackClass, LeakManagerConfig pConfig) {
        Set<LeakContainer> containers = Collections.newSetFromMap(new ConcurrentHashMap<LeakContainer, Boolean>(DEFAULT_CAPACITY));
        Map<TaskEmitterId, TaskEmitterRef> emitters = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
        Map<TCallback, TaskDescriptor> descriptors = new AutoCleanMap<>(DEFAULT_CAPACITY);

        LockFactory lockFactory = new LockFactory() {
            @Override
            public Lock create() {
                return new ReentrantLock();
            }
        };

        ThreadEnforcer threadEnforcer = new ThreadEnforcer() {
            @Override
            public void enforce() {
                // Allowed from any thread.
            }
        };

        return new AndroidLeakManager<TCallback>(lockFactory, threadEnforcer, pCallbackClass, pConfig, containers, emitters, descriptors);
    }
}