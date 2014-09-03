package com.codexperiments.leakeeper.task.android;

public class AndroidLeakManagerException extends RuntimeException {
    private static final long serialVersionUID = 1075178581665280357L;

    public AndroidLeakManagerException(String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments));
    }

    public AndroidLeakManagerException(Throwable pThrowable, String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments), pThrowable);
    }

    public static AndroidLeakManagerException emitterIdCouldNotBeDetermined(Object pTask) {
        return new AndroidLeakManagerException("Emitter Id couldn't be determined for task %1$s.", pTask);
    }

    public static AndroidLeakManagerException emitterNotManaged(Object pEmitterId, Object pEmitter) {
        return new AndroidLeakManagerException("A call to manage for emitter %2$s with Id %1$s is missing.", pEmitterId, pEmitter);
    }

    public static AndroidLeakManagerException innerTasksNotAllowed(Object pTask) {
        return new AndroidLeakManagerException("Inner tasks of type %1$s not allowed by configuration.", pTask.getClass());
    }

    public static AndroidLeakManagerException internalError() {
        return internalError(null);
    }

    public static AndroidLeakManagerException internalError(Throwable pThrowable) {
        return new AndroidLeakManagerException(pThrowable, "Internal error inside the TaskManager.");
    }

    public static AndroidLeakManagerException invalidEmitterId(Object pEmitterId, Object pEmitter) {
        return new AndroidLeakManagerException("Emitter Id %1$s is invalid for emitter %2$s.", pEmitterId, pEmitter);
    }

    public static AndroidLeakManagerException mustBeExecutedFromUIThread() {
        return new AndroidLeakManagerException("This method must be executed from the UI-Thread only.");
    }

    public static AndroidLeakManagerException taskExecutedFromUnexecutedTask(Object pEmitter) {
        return new AndroidLeakManagerException("Task executed from parent task %1$s that hasn't been executed yet.", pEmitter);
    }

    public static AndroidLeakManagerException unmanagedEmittersNotAllowed(Object pEmitter) {
        return new AndroidLeakManagerException("Unmanaged emitter forbidden by configuration (%1$s).", pEmitter);
    }
}
