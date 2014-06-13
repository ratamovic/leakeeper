package com.codexperiments.leakeeper;

import java.lang.reflect.Field;

import static com.codexperiments.leakeeper.HandlerException.internalError;

/**
 * Contains all the information necessary to restore a single emitter on a task handler (its field and its generated Id).
 */
final class HandlerEmitterDescriptor {
    private final Field mEmitterField;
    private final HandlerEmitterRef mEmitterRef;

    public HandlerEmitterDescriptor(Field pEmitterField, HandlerEmitterRef pEmitterRef) {
        mEmitterField = pEmitterField;
        mEmitterRef = pEmitterRef;
    }

    public HandlerEmitterRef hasSameType(Field pField) {
        return (pField.getType() == mEmitterField.getType()) ? mEmitterRef : null;
    }

    public boolean usesEmitter(HandlerEmitterId pHandlerEmitterId) {
        return mEmitterRef.hasSameId(pHandlerEmitterId);
    }

    /**
     * Restore reference to the current emitter on the specified task handler.
     * 
     * @param pHandler Handler to restore.
     * @return True if referencing succeeds or false else.
     */
    public boolean reference(Object pHandler) {
        try {
            Object lEmitter = mEmitterRef.get();
            if (lEmitter != null) {
                mEmitterField.set(pHandler, mEmitterRef.get());
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
     * @param pHandler Handler to dereference.
     */
    public void dereference(Object pHandler) {
        try {
            mEmitterField.set(pHandler, null);
        } catch (IllegalAccessException | RuntimeException exception) {
            throw internalError(exception);
        }
    }

    @Override
    public String toString() {
        return "TaskEmitterDescriptor [mEmitterField=" + mEmitterField + ", mEmitterRef=" + mEmitterRef + "]";
    }
}
