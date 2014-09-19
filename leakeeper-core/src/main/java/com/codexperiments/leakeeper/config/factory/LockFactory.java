package com.codexperiments.leakeeper.config.factory;

import java.util.concurrent.locks.Lock;

public interface LockFactory {
    Lock create();
}
