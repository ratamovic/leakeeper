package com.codexperiments.leakeeper;

import com.codexperiments.leakeeper.legacy.handler.TaskResult;

import java.lang.reflect.Field;

import static com.codexperiments.leakeeper.legacy.AndroidTaskManagerException.internalError;

/**
 * Contains all the information necessary to restore a single emitter on a task handler (its field and its generated Id).
 */
final class TaskEmitterDescriptor {
    private final Field mEmitterField;
    private final TaskEmitterRef mEmitterRef;

    public TaskEmitterDescriptor(Field pEmitterField, TaskEmitterRef pEmitterRef) {
        mEmitterField = pEmitterField;
        mEmitterRef = pEmitterRef;
    }

    public TaskEmitterRef hasSameType(Field pField) {
        return (pField.getType() == mEmitterField.getType()) ? mEmitterRef : null;
    }

    public boolean usesEmitter(TaskEmitterId pTaskEmitterId) {
        return mEmitterRef.hasSameId(pTaskEmitterId);
    }

    /**
     * Restore reference to the current emitter on the specified task handler.
     * 
     * @param pTaskResult Emitter to restore.
     * @return True if referencing succeed or false else.
     */
    public boolean reference(TaskResult<?> pTaskResult) {
        try {
            Object lEmitter = mEmitterRef.get();
            if (lEmitter != null) {
                mEmitterField.set(pTaskResult, mEmitterRef.get());
                return true;
            } else {
                return false;
            }
        } catch (IllegalAccessException | RuntimeException exception) {
            throw internalError(exception);
        }
    }

    /**
     * Clear reference to the given emitter on the specified task handler.
     * 
     * @param pTaskResult Emitter to dereference.
     */
    public void dereference(TaskResult<?> pTaskResult) {
        try {
            mEmitterField.set(pTaskResult, null);
        } catch (IllegalAccessException | RuntimeException exception) {
            throw internalError(exception);
        }
    }

    @Override
    public String toString() {
        return "TaskEmitterDescriptor [mEmitterField=" + mEmitterField + ", mEmitterRef=" + mEmitterRef + "]";
    }
}
