package com.codexperiments.leakeeper;

import java.lang.reflect.Field;

import static com.codexperiments.leakeeper.LeakException.internalError;

/**
 * Contains all the information necessary to restore a single emitter on a task handler (its field and its generated Id).
 */
public final class EmitterDescriptor {
    private final Field mEmitterField;
    private final EmitterRef mEmitterRef;

    public EmitterDescriptor(Field pEmitterField, EmitterRef pEmitterRef) {
        mEmitterField = pEmitterField;
        mEmitterRef = pEmitterRef;
    }

    public EmitterRef hasSameType(Field pField) {
        return (pField.getType() == mEmitterField.getType()) ? mEmitterRef : null;
    }

    public boolean usesEmitter(EmitterId pEmitterId) {
        return mEmitterRef.hasSameId(pEmitterId);
    }

    /**
     * Restore reference to the current emitter on the specified task handler.
     *
     * @param pCallback Emitter to restore.
     * @return True if referencing succeed or false else.
     */
    public boolean reference(Object pCallback) {
        try {
            Object emitter = mEmitterRef.get();
            if (emitter != null) {
                mEmitterField.set(pCallback, emitter);
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
     * @param pCallback Emitter to dereference.
     */
    public void dereference(Object pCallback) {
        try {
            mEmitterField.set(pCallback, null);
        } catch (IllegalAccessException | RuntimeException exception) {
            throw internalError(exception);
        }
    }

    @Override
    public String toString() {
        return "EmitterDescriptor [mEmitterField=" + mEmitterField + ", mEmitterRef=" + mEmitterRef + "]";
    }
}
