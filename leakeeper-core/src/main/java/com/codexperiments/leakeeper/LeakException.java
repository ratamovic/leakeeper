package com.codexperiments.leakeeper;

public class LeakException extends RuntimeException {
    private static final long serialVersionUID = 1075178581665280357L;

    public LeakException(String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments));
    }

    public LeakException(Throwable pThrowable, String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments), pThrowable);
    }

    public static LeakException emitterIdCouldNotBeDetermined(Object pTask) {
        return new LeakException("Emitter Id couldn't be determined for task %1$s.", pTask);
    }

    public static LeakException emitterNotManaged(Object pEmitterId, Object pEmitter) {
        return new LeakException("A call to manage for emitter %2$s with Id %1$s is missing.", pEmitterId, pEmitter);
    }

    public static LeakException innerTasksNotAllowed(Object pTask) {
        return new LeakException("Inner tasks of type %1$s not allowed by configuration.", pTask.getClass());
    }

    public static LeakException internalError(Throwable pThrowable) {
        return new LeakException(pThrowable, "Internal error inside the TaskManager.");
    }

    public static LeakException invalidEmitterId(Object pEmitterId, Object pEmitter) {
        return new LeakException("Emitter Id %1$s is invalid for emitter %2$s.", pEmitterId, pEmitter);
    }

    public static LeakException taskExecutedFromUnexecutedTask(Object pEmitter) {
        return new LeakException("Task executed from parent task %1$s that hasn't been executed yet.", pEmitter);
    }

    public static LeakException unmanagedEmittersNotAllowed(Object pEmitter) {
        return new LeakException("Unmanaged emitter forbidden by configuration (%1$s).", pEmitter);
    }
}
