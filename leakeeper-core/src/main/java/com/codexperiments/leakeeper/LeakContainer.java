package com.codexperiments.leakeeper;

public interface LeakContainer {
    void guard();

    boolean unguard();

    // TODO void rebind(TCallback pTaskResult);
}
