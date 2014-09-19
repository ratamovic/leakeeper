package com.codexperiments.leakeeper.task;

import java.util.concurrent.locks.Lock;

public interface LockFactory {
    Lock create();
}
