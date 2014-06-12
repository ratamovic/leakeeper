package com.codexperiments.leakeeper;

import com.codexperiments.leakeeper.legacy.*;
import com.codexperiments.leakeeper.legacy.handler.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
    // To generate task references.
    private static int TASK_REF_COUNTER;

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

    static {
        TASK_REF_COUNTER = Integer.MIN_VALUE;
    }

    public CallbackManager(TaskManagerConfig pConfig, LockingStrategy pLockingStrategy) {
        super();

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
        TaskContainer<TParam, TProgress, TResult> container = new TaskContainer<>(pTask, mDefaultScheduler);
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

    /**
     * Called when task is processed and finished to clean remaining references.
     * 
     * @param pContainer Finished task container.
     */
    protected void notifyFinished(final TaskContainer<?, ?, ?> pContainer) {
        mContainers.remove(pContainer);
    }

    /**
     * Wrapper class that contains all the information about the task to execute.
     */
    private class TaskContainer<TParam, TProgress, TResult> implements Runnable, TaskNotifier<TProgress> {
        // Handlers
        private Task<TParam, TProgress, TResult> mTask;

        // Container info.
        private volatile TaskDescriptor<TParam, TProgress, TResult> mDescriptor;
        private final TaskRef<TResult> mTaskRef;
        private final TaskId mTaskId;
        private final TaskScheduler mScheduler;

        // Task result and state.
        private TResult mResult;
        private Throwable mThrowable;
        private boolean mRunning;
        private boolean mFinished;

        public TaskContainer(Task<TParam, TProgress, TResult> pTask, TaskScheduler pScheduler) {
            super();
            mTask = pTask;

            mDescriptor = null;
            mTaskRef = new TaskRef<>(TASK_REF_COUNTER++);
            mTaskId = (pTask instanceof TaskIdentifiable) ? ((TaskIdentifiable) pTask).getId() : null;
            mScheduler = pScheduler;

            mResult = null;
            mThrowable = null;
            mRunning = true;
            mFinished = false;
        }

        /**
         * Initialize the container before running it.
         */
        protected TaskRef<TResult> prepareToRun(TaskResult<TResult> pTaskResult) {
            // Initialize the descriptor safely in its corner and dereference required values.
            final TaskDescriptor<TParam, TProgress, TResult> descriptor =
                    new TaskDescriptor<>(pTaskResult, CallbackManager.this, mLockingStrategy);
            if (!descriptor.needDereferencing(mTask)) {
                prepareTask();
            }
            // Make the descriptor visible once fully initialized.
            mDescriptor = descriptor;
            // Execute onStart() handler.
            mScheduler.scheduleIfNecessary(new Runnable() {
                public void run() {
                    descriptor.onStart(true);
                }
            });

            // Save the descriptor so that any child task can use current descriptor as a parent.
            mDescriptors.put(pTaskResult, descriptor); // TODO Global lock that could lead to contention. Check for optim.
            return mTaskRef;
        }

        /**
         * Dereference the task itself if it is disjoint from its handlers. This is definitive. No code inside the task is allowed
         * to access this$x references.
         */
        private void prepareTask() {
            try {
                Class<?> taskClass = mTask.getClass();
                while (taskClass != Object.class) {
                    // If current class is an inner class...
                    if ((taskClass.getEnclosingClass() != null) && !Modifier.isStatic(taskClass.getModifiers())) {
                        if (!mConfig.allowInnerTasks()) throw innerTasksNotAllowed(mTask);

                        // Remove any references to the outer class.
                        for (Field field : taskClass.getDeclaredFields()) {
                            if (field.getName().startsWith("this$")) {
                                field.setAccessible(true);
                                field.set(mTask, null);
                                // There should be only one outer reference per "class" in the Task class hierarchy. So we can
                                // stop as soon as the field is found as there won't be another.
                                break;
                            }
                        }
                    }
                    taskClass = taskClass.getSuperclass();
                }
            } catch (IllegalArgumentException | IllegalAccessException exception) {
                throw internalError(exception);
            }
        }

        /**
         * Run background task on Executor-thread
         */
        public void run() {
            try {
                mResult = mTask.onProcess(this);
            } catch (final Exception exception) {
                mThrowable = exception;
            } finally {
                mScheduler.schedule(new Runnable() {
                    public void run() {
                        mRunning = false;
                        finish();
                    }
                });
            }
        }

        /**
         * If a newly managed emitter is used by the present container, then call onStart() handler. Note this code allows
         * onStart() to be called even if task is rebound in-between. Thus, if a manager is restored with an emitter A and then
         * task is rebound with a new emitter B from another thread, it is possible to have onStart() called on both A and B in
         * any order. This might not be what you expect but honestly mixing rebind() and manage() in different threads is not a
         * very good practice anyway... The only real guarantee given is that onStart() will never be called after onFinish().
         * 
         * @param pEmitterId Id of the newly managed emitter. If not used by the container, nothing is done.
         */
        public void manage(TaskEmitterId pEmitterId) {
            final TaskDescriptor<TParam, TProgress, TResult> descriptor = mDescriptor;
            // Note that descriptor can be null if container has been added to the global list but hasn't been prepared yet.
            if ((descriptor != null) && descriptor.usesEmitter(pEmitterId)) {
                restore(descriptor);
            }
        }

        /**
         * Replace the previous task handler with a new one. Previous handler is lost. See manage() for concurrency concerns.
         * 
         * @param pTaskRef Reference of the task to rebind to. If different, nothing is performed.
         * @param pTaskResult Task handler that must replace previous one.
         */
        public void rebind(TaskRef<TResult> pTaskRef, TaskResult<TResult> pTaskResult) {
            if (mTaskRef.equals(pTaskRef)) {
                final TaskDescriptor<TParam, TProgress, TResult> descriptor =
                        new TaskDescriptor<>(pTaskResult, CallbackManager.this, mLockingStrategy);
                mDescriptor = descriptor;
                restore(descriptor);
                // Save the descriptor so that any child task can use current descriptor as a parent.
                mDescriptors.put(pTaskResult, descriptor); // TODO Global lock that could lead to contention. Check for optim.
            }
        }

        /**
         * Calls onStart() handler when a task is rebound to another object or when an emitter gets managed again.
         * 
         * @param pDescriptor Descriptor to use to call onStart(). Necessary to use this parameter as descriptor can be changed
         *            concurrently through rebind.
         */
        private void restore(final TaskDescriptor<TParam, TProgress, TResult> pDescriptor) {
            mScheduler.scheduleIfNecessary(new Runnable() {
                public void run() {
                    if (!finish()) {
                        pDescriptor.onStart(true);
                    }
                }
            });
        }

        /**
         * Try to execute task termination handlers (i.e. onFinish and onFail). The latter may not be executed if at least one of
         * the outer object reference can't be restored. When the task is effectively finished, the corresponding flag is set to
         * prevent any other invocation. That way, finish() can be called at any time:
         * <ul>
         * <li>When task isn't finished yet, in which case nothing happens. This can occur if a new instance of the emitter
         * becomes managed while task is still executing: the task manager try to call finish on all tasks.</li>
         * <li>When task has just finished, i.e. finish is called from the computation thread, and its emitter is available. In
         * this case, the flag is set to true and task termination handler is triggered.</li>
         * <li>When task has just finished but its emitter is not available yet, i.e. it has been unmanaged. In this case, the
         * flag is set to true but task termination handler is NOT triggered. it will be triggered later when a new emitter (with
         * the same Id) becomes managed.</li>
         * <li>When an emitter with the same Id as the previously managed-then-unmanaged one becomes managed. In this case the
         * flag is already true but task termination handler may have not been called yet (i.e. if mFinished is false). This can
         * be done now. Note hat it is possible to have finish() called several times since there may be a delay between finish()
         * call and execution as it is posted on the UI Thread.</li>
         * </ul>
         * 
         * @return True if the task could be finished and its termination handlers executed or false otherwise.
         */
        private boolean finish() {
            // Execute task termination handlers if they have not been yet (but only if the task has been fully processed).
            if (mRunning) return false;
            if (mFinished) return true;

            TaskDescriptor<TParam, TProgress, TResult> descriptor = mDescriptor;
            // TODO Don't like the configuration parameter here.
            mFinished = descriptor.onFinish(mResult, mThrowable, mConfig.keepResultOnHold(mTask));
            if (mFinished) {
                notifyFinished(this);
            }
            return mFinished;
        }

        @Override
        public void notifyProgress(final TProgress pProgress) {
            // Progress is always executed on the scheduler Thread but sent from the background Thread.
            if (!mRunning) throw progressCalledAfterTaskFinished();
            // TODO Cache.
            mScheduler.schedule(new Runnable() {
                public void run() {
                    mDescriptor.onProgress(pProgress);
                }
            });
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) return true;
            if (pOther == null) return false;
            if (getClass() != pOther.getClass()) return false;

            TaskContainer<?, ?, ?> otherContainer = (TaskContainer<?, ?, ?>) pOther;
            // Check equality on the user-defined task Id if possible.
            if (mTaskId != null) {
                return mTaskId.equals(otherContainer.mTaskId);
            }
            // If the task has no Id, then we use task equality method. This is likely to turn into a simple reference check.
            else if (mTask != null) {
                return mTask.equals(otherContainer.mTask);
            }
            // A container can't be created with a null task. So the following case should never occur.
            else {
                throw internalError();
            }
        }

        @Override
        public int hashCode() {
            if (mTaskId != null) {
                return mTaskId.hashCode();
            } else if (mTask != null) {
                return mTask.hashCode();
            } else {
                throw internalError();
            }
        }
    }
}