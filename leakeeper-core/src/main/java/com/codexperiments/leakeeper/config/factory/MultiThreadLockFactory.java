package com.codexperiments.leakeeper.config.factory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MultiThreadLockFactory implements LockFactory {
    @Override
    public Lock create() {
        return new ReentrantLock();
    }
}