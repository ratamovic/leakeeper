package com.codexperiments.leakeeper;

import com.codexperiments.leakeeper.internal.CallbackDescriptor;

import java.util.concurrent.locks.Lock;

/**
 * Wrapper class that contains all the information about the task to wrap.
 */
public class LeakContainer<TCallback> {
    private TCallback mTask; // Cannot be null
    private final CallbackDescriptor.Resolver mResolver;
    private volatile CallbackDescriptor mDescriptor = null;

    protected LeakContainer(TCallback pTask, CallbackDescriptor.Resolver pResolver) {
        super();
        mTask = pTask;
        mResolver = pResolver;
    }

    public void guard() {
        mDescriptor.dereferenceEmitter();
    }

    public boolean unguard() {
        return mDescriptor.referenceEmitter(false);
    }

    /**
     * Initialize or replace the previous task handler with a new one. Previous handler is lost.
     *
     * @param pTaskResult Task handler that must replace previous one.
     */
    CallbackDescriptor rebind(TCallback pTaskResult, Lock pLock) { // TODO XXX Duplicate parameter taskResult!!!!!
        final CallbackDescriptor lDescriptor = new CallbackDescriptor(mResolver, pTaskResult, pLock);
        mDescriptor = lDescriptor;
        return lDescriptor;
        // TODO restore(lDescriptor); Event to know when rebound.
        // Save the descriptor so that any child task can use current descriptor as a parent.
        //mDescriptors.put(pTaskResult, lDescriptor); // TODO Global lock that could lead to contention. Check for optim.
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) return true;
        if (pOther == null) return false;
        if (getClass() != pOther.getClass()) return false;

        @SuppressWarnings("unchecked")
        LeakContainer lOtherContainer = (LeakContainer) pOther;
        // If the task has no Id, then we use task equality method. This is likely to turn into a simple reference check.
        return mTask.equals(lOtherContainer.mTask);
    }

    @Override
    public int hashCode() {
        return mTask.hashCode();
    }
}
