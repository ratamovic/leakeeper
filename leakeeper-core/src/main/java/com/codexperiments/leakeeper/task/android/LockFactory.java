package com.codexperiments.leakeeper.task.android;

import java.util.concurrent.locks.Lock;

public interface LockFactory {
    Lock create();
}
