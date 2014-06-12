package com.codexperiments.leakeeper.legacy.handler;

public interface TaskStart extends TaskHandler {
    void onStart(boolean pIsRestored);
}
