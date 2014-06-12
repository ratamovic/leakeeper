package com.codexperiments.leakeeper.handler;

public interface TaskStart extends TaskHandler {
    void onStart(boolean pIsRestored);
}
