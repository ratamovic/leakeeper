package com.codexperiments.leakeeper.task.factory;

import android.os.Looper;
import com.codexperiments.leakeeper.task.*;
import com.codexperiments.leakeeper.task.impl.LeakManagerImpl;
import com.codexperiments.leakeeper.task.impl.*;
import com.codexperiments.leakeeper.task.util.AutoCleanMap;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static com.codexperiments.leakeeper.task.impl.LeakManagerException.mustBeExecutedFromUIThread;

/**
 * Everything is done on the UI-Thread. No lock required.
 */
public class UIThreadLeakManagerFactory implements LeakManagerFactory {
    private static final int DEFAULT_CAPACITY = 64;

    @Override
    public <TCallback> LeakManagerImpl<TCallback> createManager(Class<TCallback> pCallbackClass, LeakManagerConfig pConfig) {
        Set<LeakContainer> containers = new HashSet<>(DEFAULT_CAPACITY);
        Map<TaskEmitterId, TaskEmitterRef> emitters = new HashMap<>(DEFAULT_CAPACITY);
        Map<TCallback, TaskDescriptor> descriptors = new AutoCleanMap<>(DEFAULT_CAPACITY);

        LockFactory lockFactory = new LockFactory() {
            private final EmptyLock emptyLock = new EmptyLock();

            @Override
            public Lock create() {
                return emptyLock;
            }
        };

        ThreadEnforcer threadEnforcer = new ThreadEnforcer() {
            private final Looper mUILooper = Looper.getMainLooper();

            @Override
            public void enforce() {
                if (Looper.myLooper() != mUILooper) throw mustBeExecutedFromUIThread();
            }
        };

        return new LeakManagerImpl<TCallback>(lockFactory, threadEnforcer, pCallbackClass, pConfig, containers, emitters, descriptors);
    }

    private static class EmptyLock implements Lock {
        @Override
        public void lock() {
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long pTime, TimeUnit pUnit) throws InterruptedException {
            return true;
        }

        @Override
        public void unlock() {
        }
    }
}
