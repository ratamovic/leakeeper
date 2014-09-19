package com.codexperiments.leakeeper.task.impl;

public class LeakManagerException extends RuntimeException {
    private static final long serialVersionUID = 1075178581665280357L;

    public LeakManagerException(String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments));
    }

    public LeakManagerException(Throwable pThrowable, String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments), pThrowable);
    }

    public static LeakManagerException emitterIdCouldNotBeDetermined(Object pTask) {
        return new LeakManagerException("Emitter Id couldn't be determined for task %1$s.", pTask);
    }

    public static LeakManagerException emitterNotManaged(Object pEmitterId, Object pEmitter) {
        return new LeakManagerException("A call to manage for emitter %2$s with Id %1$s is missing.", pEmitterId, pEmitter);
    }

    public static LeakManagerException innerTasksNotAllowed(Object pTask) {
        return new LeakManagerException("Inner tasks of type %1$s not allowed by configuration.", pTask.getClass());
    }

    public static LeakManagerException internalError(Throwable pThrowable) {
        return new LeakManagerException(pThrowable, "Internal error inside the TaskManager.");
    }

    public static LeakManagerException invalidEmitterId(Object pEmitterId, Object pEmitter) {
        return new LeakManagerException("Emitter Id %1$s is invalid for emitter %2$s.", pEmitterId, pEmitter);
    }

    public static LeakManagerException mustBeExecutedFromUIThread() {
        return new LeakManagerException("This method must be executed from the UI-Thread only.");
    }

    public static LeakManagerException taskExecutedFromUnexecutedTask(Object pEmitter) {
        return new LeakManagerException("Task executed from parent task %1$s that hasn't been executed yet.", pEmitter);
    }

    public static LeakManagerException unmanagedEmittersNotAllowed(Object pEmitter) {
        return new LeakManagerException("Unmanaged emitter forbidden by configuration (%1$s).", pEmitter);
    }
}
