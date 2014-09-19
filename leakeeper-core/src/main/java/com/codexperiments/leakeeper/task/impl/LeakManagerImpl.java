package com.codexperiments.leakeeper.task.impl;

import com.codexperiments.leakeeper.task.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static com.codexperiments.leakeeper.task.impl.LeakManagerException.*;

/**
 * Terminology:
 *
 * <ul>
 * <li>Emitter: A task emitter is, in Java terms, an outer class object that requests a task to execute. Thus, a task can have
 * emitters only if it is an inner, local or anonymous class. It's important to note that an object can have one or several
 * emitters since this is allowed by the Java language (an inner class can keep reference to several enclosing class).</li>
 * <li>Dereferencing: An inner class task keeps references to its emitters. These references must be removed temporarily during
 * processing to avoid possible memory leaks (e.g. if a task references an activity that gets destroyed during processing).</li>
 * <li>Referencing: References to emitters must be restored to execute task handlers (onFinish(), onFail(), onProgress()) or else,
 * the task would be unable to communicate with the outside world since it has be dereferenced. Referencing is possible only if
 * all the necessary emitters, managed by the LeakManager, are still reachable. If not, task handlers cannot be executed until all
 * are reachable (and if configuration requires to keep results on hold).</li>
 * </ul>
 *
 * <b>The problem:</b>
 *
 * There are many ways to handle asynchronous tasks in Android to load data or perform some background processing. Several ways to
 * handle this exist, among which:
 * <ul>
 * <li>AsyncTasks: One of the most efficient ways to write asynchronous tasks but also to make mistakes. Can be used as such
 * mainly for short-lived tasks (or by using WeakReferences).</li>
 * <li>Services or IntentServices (with Receivers): Probably the most flexible and safest way to handle asynchronous tasks, but
 * requires some boilerplate "plumbing"</li>
 * <li>Loaders: which are tied to the activity life-cycle and also a bit difficult to write when handling all the specific cases
 * that may occur (Loader reseted, etc.). They require less plumbing but still some.</li>
 * <li>Content Providers: which are just nice to use to create a remote data source. Cumbersome and annoying to write for any
 * other use... And they are not inherently threaded anyway.</li>
 * <li>...</li>
 * </ul>
 * Each technique has its drawbacks. The most practical way, AsyncTasks, can easily cause memory leaks which occur especially with
 * inner classes which keep a reference to the outer object. A typical example is an Activity referenced from an inner AsyncTask:
 * when the Activity is destroyed because of a configuration change (e.g. screen rotation) or because user leave the Activity
 * (e.g. with Home button), then the executing AsyncTask still references its containing Activity which cannot be garbage
 * collected. Even worse, accessing the emitting Activity after AsyncTask is over may cause either no result at all or exceptions,
 * because a new version of the Activity may have been created in-between and the older one is not displayed any more or has freed
 * some resources.
 *
 * <b>How it works:</b>
 *
 * As soon as a task is enqueued in execute(), all its emitters are dereferenced to avoid any possible memory leaks during
 * processing (in Task.onProcess()). In other words, any emitters (i.e. outer class references) are replaced with null. This means
 * that your Task:
 * <ul>
 * <li>Can execute safely without memory leaks. Activity or any other emitter can still be garbage collected.</li>
 * <li><b>CANNOT access outer emitters (again, any outer class reference) from the onProcess method() or must use a static
 * Task!</b> That's price to pay for this memory safety... Use allowInnerTasks() in Configuration object to forbid the use of
 * inner tasks.</li>
 * <li><b>Any member variables need by a Task must be copied in Task constructor.</b> That way, the Task can work safely in a
 * closed environment without the interference of other threads. Indeed, don't share any variable between onProcess() and any
 * other threads, UI-Thread included, as this could lead to unpredictable result (because of Thread caching or instruction
 * reordering) unless some synchronization is performed (which can lead to bottleneck or a dead lock in extreme case if not
 * appropriately handled).</li>
 * </ul>
 *
 * Before, during or after processing, several handlers (i.e. callbacks) can be called:
 * <ul>
 * <li>onStart()</li>
 * <li>onProgress()</li>
 * <li>onFinish()</li>
 * <li>onFail()</li>
 * </ul>
 * Right before and after these handlers are invoked, emitters are respectively referenced and dereferenced to allow accessing the
 * outer class. If outer class is not available (e.g. if Activity has been destroyed but not recreated yet).
 *
 * TODO Remove TaskId but create a TaskEquality helper class.
 * 
 * TODO Handle cancellation.
 * 
 * TODO onBeforeProcess / onRestore / onCommit
 * 
 * TODO Save TaskRefs list.
 * 
 * TODO TaskRef add a Tag
 * 
 * TODO pending(TaskType)
 */
public class LeakManagerImpl<TCallback> implements LeakManager<TCallback> {
    /*TODO*/ final private Class<TCallback> mCallbackClass;
    private final LockFactory mLockFactory;
    private final ThreadEnforcer mThreadEnforcer;
    private LeakManagerConfig mConfig;
    // All the current running tasks.
    private Set<LeakContainer> mContainers;
    // Keep tracks of all emitters. Note that TaskEmitterRef uses a weak reference to avoid memory leaks. This Map is never
    // cleaned and accumulates references because it assumes that any object that managed object set doesn't grow infinitely but
    // is rather limited (e.g. typically all fragments, activity and manager in an Application).
    private Map<TaskEmitterId, TaskEmitterRef> mEmitters;
    // Allow getting back an existing descriptor through its handler when dealing with nested tasks. An AutoCleanMap is necessary
    // since there is no way to know when a handler are not necessary anymore.
    /*private*/ Map<TCallback, TaskDescriptor> mDescriptors; // TODO Handle this with a counter in descriptor?


    public LeakManagerImpl(LockFactory pLockFactory, ThreadEnforcer pThreadEnforcer,
                           Class<TCallback> pCallbackClass, LeakManagerConfig pConfig, Set<LeakContainer> pContainers,
                           Map<TaskEmitterId, TaskEmitterRef> pEmitters, Map<TCallback, TaskDescriptor> pDescriptors) {
        super();

        mLockFactory = pLockFactory;
        mThreadEnforcer = pThreadEnforcer;
        mCallbackClass = pCallbackClass;
        mConfig = pConfig;
        mContainers = pContainers;
        mEmitters = pEmitters;
        mDescriptors = pDescriptors;
    }

    @Override
    public void manage(Object pEmitter) {
        if (pEmitter == null) throw new NullPointerException("Emitter is null");
        mThreadEnforcer.enforce();

        // Save the new emitter in the reference list. Replace the existing one, if any, according to its id (the old one is
        // considered obsolete). Emitter Id is computed by the configuration and can be null if emitter is not managed.
        Object lEmitterIdValue = mConfig.resolveEmitterId(pEmitter);
        // Emitter id must not be the emitter itself or we have a leak. Warn user about this (tempting) configuration misuse.
        if ((lEmitterIdValue == null) || (lEmitterIdValue == pEmitter)) throw invalidEmitterId(lEmitterIdValue, pEmitter);

        // Save the reference of the emitter. Initialize it lazily if it doesn't exist.
        TaskEmitterId lEmitterId = new TaskEmitterId(pEmitter.getClass(), lEmitterIdValue);
        TaskEmitterRef lEmitterRef = mEmitters.get(lEmitterId);
        if (lEmitterRef == null) {
            /*lEmitterRef =*/ mEmitters.put(lEmitterId, new TaskEmitterRef(lEmitterId, pEmitter));
        } else {
            lEmitterRef.set(pEmitter);
        }
    }

    @Override
    public void unmanage(Object pEmitter) {
        if (pEmitter == null) throw new NullPointerException("Emitter is null");
        mThreadEnforcer.enforce();

        // Remove an existing task emitter. If the emitter reference (in Java terms) is different from the object to remove, then
        // don't do anything. This could occur if a new object is managed before an older one with the same Id is unmanaged.
        // Typically, this could occur for example if an Activity X starts and then navigates to an Activity B which is,
        // according to Android lifecycle, started before A is stopped (the two activities are alive at the same time during a
        // short period of time).
        // TODO (lEmitterRef.get() == pEmitter) is not a proper way to handle unmanage() when dealing with activities since this
        // can lead to concurrency defects. It would be better to force call to unmanage().
        Object lEmitterIdValue = mConfig.resolveEmitterId(pEmitter);
        if (lEmitterIdValue != null) {
            TaskEmitterId lEmitterId = new TaskEmitterId(pEmitter.getClass(), lEmitterIdValue);
            TaskEmitterRef lEmitterRef = mEmitters.get(lEmitterId);
            if ((lEmitterRef != null) && (lEmitterRef.get() == pEmitter)) {
                lEmitterRef.clear();
            }
        }
    }

    @Override
    public LeakContainer wrap(TCallback pCallback) {
        if (pCallback == null) throw new NullPointerException("Callback is null");
        mThreadEnforcer.enforce();

        // Create a container to run the task.
        LeakContainerImpl lContainer = new LeakContainerImpl(pCallback);
        // Save the task before running it.
        // Note that it is safe to add the task to the container since it is an empty stub that shouldn't create any side-effect.
        if (mContainers.add(lContainer)) {
            // Prepare the task (i.e. initialize and cache needed values) after adding it because prepareToRun() is a bit
            // expensive and should be performed only if necessary.
            try {
                lContainer.rebind(pCallback);
                return lContainer;
            }
            // If preparation operation fails, try to leave the manager in a consistent state without memory leaks.
            catch (RuntimeException eRuntimeException) {
                mContainers.remove(lContainer);
                throw eRuntimeException;
            }
        }
        // If an identical task is already executing, do nothing.
        else return null;
    }

    /**
     * Called internally when initializing a TaskDescriptor to a reference to an emitter, either managed or not. If the emitter is
     * not managed, then return an unmanaged reference (i.e. that is not stored in mEmitters).
     *
     * @param pEmitter Emitter to find the reference of.
     * @return Emitter reference. No null is returned.
     */
    protected TaskEmitterRef resolveEmitterRef(Object pEmitter) {
        // Save the new emitter in the reference list. Replace the existing one, if any, according to its id (the old one is
        // considered obsolete). Emitter Id is computed by the configuration strategy. Note that an emitter Id can be null if no
        // dereferencing should be performed.
        Object lEmitterIdValue = mConfig.resolveEmitterId(pEmitter);
        // Emitter id must not be the emitter itself or we have a leak. Warn user about this (tempting) configuration misuse.
        // Note that when we arrive here, pEmitter can't be null.
        if (lEmitterIdValue == pEmitter) throw invalidEmitterId(lEmitterIdValue, pEmitter);

        TaskEmitterRef lEmitterRef;
        // Managed emitter case.
        if (lEmitterIdValue != null) {
            TaskEmitterId lEmitterId = new TaskEmitterId(pEmitter.getClass(), lEmitterIdValue);
            lEmitterRef = mEmitters.get(lEmitterId);
            // If emitter is managed by the user explicitly and is properly registered in the emitter list, do nothing. User can
            // update reference himself through manage(Object) later. But if emitter is managed (i.e. emitter Id returned by
            // configuration is not null) but is not in the emitter list, then a call to manage() is missing. Warn the user.
            if (lEmitterRef == null) throw emitterNotManaged(lEmitterIdValue, pEmitter);
        }
        // Unmanaged emitter case.
        else {
            if (!mConfig.allowUnmanagedEmitters()) throw unmanagedEmittersNotAllowed(pEmitter);
            // TODO This is wrong! There should be only one TaskEmitterRef per emitter or concurrency problems may occur.
            lEmitterRef = new TaskEmitterRef(pEmitter);
        }
        return lEmitterRef;
    }

    protected TaskDescriptor resolveDescriptor(Field pField, Object pEmitter) {
        if (!mCallbackClass.isAssignableFrom(pField.getType())) return null;

        @SuppressWarnings("SuspiciousMethodCalls")
        TaskDescriptor lDescriptor = mDescriptors.get(pEmitter);
        if (lDescriptor == null) return lDescriptor;
        else throw taskExecutedFromUnexecutedTask(pEmitter);
    }

    /**
     * Called when task is processed and finished to clean remaining references.
     *
     * @param pContainer Finished task container.
     */
    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public void unwrap(LeakContainer pContainer) {
        mContainers.remove(pContainer);
    }

    /**
     * Wrapper class that contains all the information about the task to wrap.
     */
    private class LeakContainerImpl implements LeakContainer {
        // Handlers
        private TCallback mTask; // Cannot be null
        // Container info.
        private volatile TaskDescriptor mDescriptor = null;

        public LeakContainerImpl(TCallback pTask) {
            super();
            mTask = pTask;
        }

        @Override
        public void guard() {
            mDescriptor.dereferenceEmitter();
        }

        @Override
        public boolean unguard() {
            return mDescriptor.referenceEmitter(false);
        }

        /**
         * Initialize or replace the previous task handler with a new one. Previous handler is lost.
         *
         * @param pTaskResult Task handler that must replace previous one.
         */
        public void rebind(TCallback pTaskResult) {
            final TaskDescriptor lDescriptor = new TaskDescriptor(pTaskResult, mLockFactory.create());
            mDescriptor = lDescriptor;
            // TODO restore(lDescriptor); Event to know when rebound.
            // Save the descriptor so that any child task can use current descriptor as a parent.
            mDescriptors.put(pTaskResult, lDescriptor); // TODO Global lock that could lead to contention. Check for optim.
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) return true;
            if (pOther == null) return false;
            if (getClass() != pOther.getClass()) return false;

            @SuppressWarnings("unchecked")
            LeakContainerImpl lOtherContainer = (LeakContainerImpl) pOther;
            // If the task has no Id, then we use task equality method. This is likely to turn into a simple reference check.
            return mTask.equals(lOtherContainer.mTask);
        }

        @Override
        public int hashCode() {
            return mTask.hashCode();
        }
    }
}