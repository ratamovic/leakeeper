package com.codexperiments.leakeeper.legacy;

import com.codexperiments.leakeeper.legacy.handler.Task;
import com.codexperiments.leakeeper.legacy.handler.TaskIdentifiable;
import com.codexperiments.leakeeper.legacy.handler.TaskNotifier;
import com.codexperiments.leakeeper.legacy.handler.TaskResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static com.codexperiments.leakeeper.legacy.AndroidTaskManagerException.internalError;
import static com.codexperiments.leakeeper.legacy.AndroidTaskManagerException.progressCalledAfterTaskFinished;

/**
 * Wrapper class that contains all the information about the task to execute.
 */
class TaskContainer<TParam, TProgress, TResult> implements Runnable, TaskNotifier<TProgress> {
    // To generate task references.
    private static int TASK_REF_COUNTER;

    static {
        TASK_REF_COUNTER = Integer.MIN_VALUE;
    }

    private TaskDescriptorFactory mTaskDescriptorFactory;
    private TaskResolver mTaskResolver;
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

    public TaskContainer(TaskDescriptorFactory pTaskDescriptorFactory, Task<TParam, TProgress, TResult> pTask,
                         TaskScheduler pScheduler, TaskResolver pTaskResolver) {
        super();
        mTaskDescriptorFactory = pTaskDescriptorFactory;
        mTaskResolver = pTaskResolver;
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
        final TaskDescriptor<TParam, TProgress, TResult> descriptor = mTaskDescriptorFactory.create(pTaskResult, mTaskResolver);
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
        mTaskResolver.saveDescriptor(pTaskResult, descriptor);
//        mDescriptors.put(pTaskResult, descriptor); // TODO Global lock that could lead to contention. Check for optim.
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
                    //if (!mConfig.allowInnerTasks()) throw innerTasksNotAllowed(mTask);

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
            final TaskDescriptor<TParam, TProgress, TResult> descriptor = mTaskDescriptorFactory.create(pTaskResult, mTaskResolver);
            mDescriptor = descriptor;
            restore(descriptor);
            // Save the descriptor so that any child task can use current descriptor as a parent.
            mTaskResolver.saveDescriptor(pTaskResult, descriptor);
//            mDescriptors.put(pTaskResult, descriptor); // TODO Global lock that could lead to contention. Check for optim.
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
        mFinished = descriptor.onFinish(mResult, mThrowable, false/*mConfig.keepResultOnHold(mTask)*/);
        if (mFinished) {
            mTaskResolver.removeDescriptor(this);
//            notifyFinished(this);
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
