package com.codexperiments.leakeeper.legacy;

import com.codexperiments.leakeeper.legacy.handler.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.codexperiments.leakeeper.legacy.AndroidTaskManagerException.*;

/**
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
public class CallbackManager implements TaskResolver {
    private static final int DEFAULT_CAPACITY = 64;

    private TaskDescriptorFactory mDefaultTaskDescriptorFactory;
    private TaskScheduler mDefaultScheduler;
    private LockingStrategy mLockingStrategy;
    private TaskManagerConfig mConfig;
    // All the current running tasks.
    private Set<TaskContainer<?, ?, ?>> mContainers;
    // Keep tracks of all emitters. Note that TaskEmitterRef uses a weak reference to avoid memory leaks. This Map is never
    // cleaned and accumulates references because it assumes that any object that managed object set doesn't grow infinitely but
    // is rather limited (e.g. typically all fragments, activity and manager in an Application).
    private Map<TaskEmitterId, TaskEmitterRef> mEmitters;
    // Allows getting back an existing descriptor through its handler when dealing with nested tasks. An AutoCleanMap is necessary
    // since there is no way to know when a handler are not necessary anymore.
    private Map<TaskHandler, TaskDescriptor<?, ?, ?>> mDescriptors;

    public CallbackManager(TaskManagerConfig pConfig, LockingStrategy pLockingStrategy) {
        super();

        mDefaultTaskDescriptorFactory = new DefaultTaskDescriptorFactory(pLockingStrategy);
        mDefaultScheduler = new AndroidUITaskScheduler();
        mConfig = pConfig;
        mLockingStrategy = pLockingStrategy;
        mLockingStrategy.createManager(this);
        mContainers = Collections.newSetFromMap(new ConcurrentHashMap<TaskContainer<?, ?, ?>, Boolean>(DEFAULT_CAPACITY));
        mEmitters = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
        mDescriptors = new AutoCleanMap<>(DEFAULT_CAPACITY);
    }

//    @Override
    public void manage(Object pEmitter) {
        if (pEmitter == null) throw new NullPointerException("Emitter is null");
        mLockingStrategy.checkCallIsAllowed();

        // Save the new emitter in the reference list. Replace the existing one, if any, according to its id (the old one is
        // considered obsolete). Emitter Id is computed by the configuration and can be null if emitter is not managed.
        Object emitterIdValue = mConfig.resolveEmitterId(pEmitter);
        // Emitter id must not be the emitter itself or we have a leak. Warn user about this (tempting) configuration misuse.
        if ((emitterIdValue == null) || (emitterIdValue == pEmitter)) throw invalidEmitterId(emitterIdValue, pEmitter);

        // Save the reference of the emitter. Initialize it lazily if it doesn't exist.
        TaskEmitterId EmitterId = new TaskEmitterId(pEmitter.getClass(), emitterIdValue);
        TaskEmitterRef emitterRef = mEmitters.get(EmitterId);
        if (emitterRef == null) {
            emitterRef = mEmitters.put(EmitterId, new TaskEmitterRef(EmitterId, pEmitter));
        } else {
            emitterRef.set(pEmitter);
        }

        // Try to terminate any task we can, which is possible if the newly managed emitter is one of their emitter.
        // TODO Maybe we should add a reference from the TaskEmitterId to the containers to avoid this lookup.
        for (TaskContainer<?, ?, ?> container : mContainers) {
            container.manage(EmitterId);
        }
    }

//    @Override
    public void unmanage(Object pEmitter) {
        if (pEmitter == null) throw new NullPointerException("Emitter is null");
        mLockingStrategy.checkCallIsAllowed();

        // Remove an existing task emitter. If the emitter reference (in Java terms) is different from the object to remove, then
        // don't do anything. This could occur if a new object is managed before an older one with the same Id is unmanaged.
        // Typically, this could occur for example if an Activity X starts and then navigates to an Activity B which is,
        // according to Android lifecycle, started before A is stopped (the two activities are alive at the same time during a
        // short period of time).
        // TODO (lEmitterRef.get() == pEmitter) is not a proper way to handle unmanage() when dealing with activities since this
        // can lead to concurrency defects. It would be better to force call to unmanage().
        Object emitterIdValue = mConfig.resolveEmitterId(pEmitter);
        if (emitterIdValue != null) {
            TaskEmitterId emitterId = new TaskEmitterId(pEmitter.getClass(), emitterIdValue);
            TaskEmitterRef emitterRef = mEmitters.get(emitterId);
            if ((emitterRef != null) && (emitterRef.get() == pEmitter)) {
                emitterRef.clear();
            }
        }
    }

//    @Override
    public <TParam, TProgress, TResult> TaskRef<TResult> execute(Task<TParam, TProgress, TResult> pTask) {
        return execute(pTask, pTask);
    }

//    @Override
    public <TParam, TProgress, TResult> TaskRef<TResult> execute(Task<TParam, TProgress, TResult> pTask,
                                                                 TaskResult<TResult> pTaskResult)
    {
        if (pTask == null) throw new NullPointerException("Task is null");
        if (pTaskResult == null) throw new NullPointerException("TaskResult is null");
        mLockingStrategy.checkCallIsAllowed();

        // Create a container to run the task.
        TaskContainer<TParam, TProgress, TResult> container = new TaskContainer<>(mDefaultTaskDescriptorFactory, pTask, mDefaultScheduler, this);
        // Save the task before running it.
        // Note that it is safe to add the task to the container since it is an empty stub that shouldn't create any side-effect.
        if (mContainers.add(container)) {
            // Prepare the task (i.e. initialize and cache needed values) after adding it because prepareToRun() is a bit
            // expensive and should be performed only if necessary.
            try {
                TaskRef<TResult> taskRef = container.prepareToRun(pTaskResult);
                mConfig.resolveExecutor(pTask).execute(container);
                return taskRef;
            }
            // If preparation operation fails, try to leave the manager in a consistent state without memory leaks.
            catch (RuntimeException runtimeException) {
                mContainers.remove(container);
                throw runtimeException;
            }
        }
        // If an identical task is already executing, do nothing.
        else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <TParam, TProgress, TResult> boolean rebind(TaskRef<TResult> pTaskRef, TaskResult<TResult> pTaskResult) {
        if (pTaskRef == null) throw new NullPointerException("Task is null");
        if (pTaskResult == null) throw new NullPointerException("TaskResult is null");
        mLockingStrategy.checkCallIsAllowed();

        // TODO It seems possible to add a reference from the TaskRef to the TaskContainer to avoid this lookup.
        for (TaskContainer<?, ?, ?> container : mContainers) {
            // Cast safety is guaranteed by the execute() method that returns a properly typed TaskRef for a new container.
            ((TaskContainer<TParam, TProgress, TResult>) container).rebind(pTaskRef, pTaskResult);
            return true;
        }
        return false;
    }

//    @Override
    public void notifyProgress() {
        throw notCalledFromTask();
    }

    /**
     * Called internally when initializing a TaskDescriptor to a reference to an emitter, either managed or not. If the emitter is
     * not managed, then return an unmanaged reference (i.e. that is not stored in mEmitters).
     * 
     * @param pEmitter Emitter to find the reference of.
     * @return Emitter reference. No null is returned.
     */
    @Override
    public TaskEmitterRef resolveRef(Object pEmitter) {
        // Save the new emitter in the reference list. Replace the existing one, if any, according to its id (the old one is
        // considered obsolete). Emitter Id is computed by the configuration strategy. Note that an emitter Id can be null if no
        // dereferencing should be performed.
        Object emitterIdValue = mConfig.resolveEmitterId(pEmitter);
        // Emitter id must not be the emitter itself or we have a leak. Warn user about this (tempting) configuration misuse.
        // Note that when we arrive here, pEmitter can't be null.
        if (emitterIdValue == pEmitter) throw invalidEmitterId(emitterIdValue, pEmitter);

        TaskEmitterRef emitterRef;
        // Managed emitter case.
        if (emitterIdValue != null) {
            TaskEmitterId emitterId = new TaskEmitterId(pEmitter.getClass(), emitterIdValue);
            emitterRef = mEmitters.get(emitterId);
            // If emitter is managed by the user explicitly and is properly registered in the emitter list, do nothing. User can
            // update reference himself through manage(Object) later. But if emitter is managed (i.e. emitter Id returned by
            // configuration is not null) but is not in the emitter list, then a call to manage() is missing. Warn the user.
            if (emitterRef == null) throw emitterNotManaged(emitterIdValue, pEmitter);
        }
        // Unmanaged emitter case.
        else {
            if (!mConfig.allowUnmanagedEmitters()) throw unmanagedEmittersNotAllowed(pEmitter);
            // TODO This is wrong! There should be only one TaskEmitterRef per emitter or concurrency problems may occur.
            emitterRef = new TaskEmitterRef(pEmitter);
        }
        return emitterRef;
    }

    @Override
    public TaskDescriptor resolveDescriptor(Object pEmitter) {
        return mDescriptors.get(pEmitter);
    }

    @Override
    public <TResult> TaskDescriptor saveDescriptor(TaskResult<TResult> pTaskResult, TaskDescriptor<?, ?, TResult> pDescriptor) {
        return mDescriptors.put(pTaskResult, pDescriptor);
    }

    /**
     * Called when task is processed and finished to clean remaining references.
     *
     * @param pContainer Finished task container.
     */
    @Override
    public void removeDescriptor(TaskContainer<?, ?, ?> pContainer) {
        mContainers.remove(pContainer);
    }
//
//    /**
//     * Called when task is processed and finished to clean remaining references.
//     *
//     * @param pContainer Finished task container.
//     */
//    protected void notifyFinished(final TaskContainer<?, ?, ?> pContainer) {
//        mContainers.remove(pContainer);
//    }
}