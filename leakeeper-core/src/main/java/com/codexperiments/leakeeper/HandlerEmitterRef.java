package com.codexperiments.leakeeper;

import java.lang.ref.WeakReference;

/**
 * Represents a reference to an emitter. Its goal is to add a level of indirection to the emitter so that several tasks can
 * easily share updates made to an emitter.
 */
final class HandlerEmitterRef {
    private final HandlerEmitterId mEmitterId;
    private volatile WeakReference<Object> mEmitterRef;

    public HandlerEmitterRef(Object pEmitterValue) {
        mEmitterId = null;
        set(pEmitterValue);
    }

    public HandlerEmitterRef(HandlerEmitterId pEmitterId, Object pEmitterValue) {
        mEmitterId = pEmitterId;
        set(pEmitterValue);
    }

    public boolean hasSameId(HandlerEmitterId pHandlerEmitterId) {
        return (mEmitterId != null) && mEmitterId.equals(pHandlerEmitterId);
    }

    public Object get() {
        return (mEmitterRef != null) ? mEmitterRef.get() : null;
    }

    public void set(Object pEmitterValue) {
        mEmitterRef = new WeakReference<>(pEmitterValue);
    }

    public void clear() {
        mEmitterRef = null;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) return true;
        if (pOther == null) return false;
        if (getClass() != pOther.getClass()) return false;

        HandlerEmitterRef lOther = (HandlerEmitterRef) pOther;
        if (mEmitterId == null) return lOther.mEmitterId == null;
        else return mEmitterId.equals(lOther.mEmitterId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mEmitterId == null) ? 0 : mEmitterId.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "TaskEmitterRef [mEmitterId=" + mEmitterId + ", mEmitterRef=" + mEmitterRef + "]";
    }
}
