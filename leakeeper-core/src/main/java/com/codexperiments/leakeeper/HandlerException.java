package com.codexperiments.leakeeper;

import com.codexperiments.leakeeper.legacy.handler.Task;
import com.codexperiments.leakeeper.legacy.handler.TaskResult;

public class HandlerException extends RuntimeException {
    private static final long serialVersionUID = 1075178581665280357L;

    public HandlerException(String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments));
    }

    public HandlerException(Throwable pThrowable, String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments), pThrowable);
    }

    public static HandlerException emitterIdCouldNotBeDetermined(Object pHandler) {
        return new HandlerException("Invalid handler %1$s : Emitter Id couldn't be bound.", pHandler);
    }

    public static HandlerException emitterNotManaged(Object pEmitterId, Object pEmitter) {
        return new HandlerException("A call to manage for emitter %2$s with Id %1$s is missing.", pEmitterId, pEmitter);
    }

    public static HandlerException innerTasksNotAllowed(Task<?, ?, ?> pTask) {
        return new HandlerException("Inner tasks like %1$s not allowed by configuration.", pTask.getClass());
    }

    public static HandlerException internalError() {
        return internalError(null);
    }

    public static HandlerException internalError(Throwable pThrowable) {
        return new HandlerException(pThrowable, "Internal error inside the TaskManager.");
    }

    public static HandlerException invalidEmitterId(Object pEmitterId, Object pEmitter) {
        return new HandlerException("Emitter Id %1$s is invalid for emitter %2$s.", pEmitterId, pEmitter);
    }

    public static HandlerException mustBeExecutedFromUIThread() {
        return new HandlerException("This method must be executed from the UI-Thread only.");
    }

    public static HandlerException notCalledFromTask() {
        return new HandlerException("This operation must be called inside a task.");
    }

    public static HandlerException progressCalledAfterTaskFinished() {
        return new HandlerException("notifyProgress() called after task finished.");
    }

    public static HandlerException taskExecutedFromUnexecutedTask(Object pEmitter) {
        return new HandlerException("Task executed from parent task %1$s that hasn't been executed yet.", pEmitter);
    }

    public static HandlerException unmanagedEmittersNotAllowed(Object pEmitter) {
        return new HandlerException("Unmanaged emitter forbidden by configuration (%1$s).", pEmitter);
    }
}
