package com.codexperiments.leakeeper.config.factory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class SingleThreadLockFactory implements LockFactory {
    private final EmptyLock emptyLock = new EmptyLock();

    @Override
    public Lock create() {
        return emptyLock;
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