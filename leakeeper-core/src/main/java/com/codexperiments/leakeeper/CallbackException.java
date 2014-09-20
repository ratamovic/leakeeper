package com.codexperiments.leakeeper;

public class CallbackException extends RuntimeException {
    private static final long serialVersionUID = 1075178581665280357L;

    public CallbackException(String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments));
    }

    public CallbackException(Throwable pThrowable, String pMessage, Object... pArguments) {
        super(String.format(pMessage, pArguments), pThrowable);
    }

    public static CallbackException emitterIdCouldNotBeDetermined(Object pTask) {
        return new CallbackException("Emitter Id couldn't be determined for task %1$s.", pTask);
    }

    public static CallbackException emitterNotManaged(Object pEmitterId, Object pEmitter) {
        return new CallbackException("A call to manage for emitter %2$s with Id %1$s is missing.", pEmitterId, pEmitter);
    }

    public static CallbackException innerTasksNotAllowed(Object pTask) {
        return new CallbackException("Inner tasks of type %1$s not allowed by configuration.", pTask.getClass());
    }

    public static CallbackException internalError(Throwable pThrowable) {
        return new CallbackException(pThrowable, "Internal error inside the TaskManager.");
    }

    public static CallbackException invalidEmitterId(Object pEmitterId, Object pEmitter) {
        return new CallbackException("Emitter Id %1$s is invalid for emitter %2$s.", pEmitterId, pEmitter);
    }

    public static CallbackException taskExecutedFromUnexecutedTask(Object pEmitter) {
        return new CallbackException("Task executed from parent task %1$s that hasn't been executed yet.", pEmitter);
    }

    public static CallbackException unmanagedEmittersNotAllowed(Object pEmitter) {
        return new CallbackException("Unmanaged emitter forbidden by configuration (%1$s).", pEmitter);
    }
}
