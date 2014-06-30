package com.codexperiments.leakeeper.task.handler;

public interface TaskStart extends TaskHandler {
    void onStart(boolean pIsRestored);
}
