package com.codexperiments.leakeeper.task.android;

import android.os.Looper;
import com.codexperiments.leakeeper.task.*;
import com.codexperiments.leakeeper.task.util.AutoCleanMap;
import com.codexperiments.leakeeper.task.util.EmptyLock;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.codexperiments.leakeeper.task.android.AndroidLeakManagerException.*;

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
public class AndroidLeakManager<TCallback> implements LeakManager<TCallback> {
    private static final int DEFAULT_CAPACITY = 64;
    // To generate task references.
    private static int TASK_REF_COUNTER;

    /*TODO*/ final private Class<TCallback> mCallbackClass;
    private TaskScheduler mDefaultScheduler;
    private LockingStrategy mLockingStrategy;
    private LeakManagerConfig mConfig;
    // All the current running tasks.
    private Set<TaskContainer> mContainers;
    // Keep tracks of all emitters. Note that TaskEmitterRef uses a weak reference to avoid memory leaks. This Map is never
    // cleaned and accumulates references because it assumes that any object that managed object set doesn't grow infinitely but
    // is rather limited (e.g. typically all fragments, activity and manager in an Application).
    private Map<TaskEmitterId, TaskEmitterRef> mEmitters;
    // Allow getting back an existing descriptor through its handler when dealing with nested tasks. An AutoCleanMap is necessary
    // since there is no way to know when a handler are not necessary anymore.
    private Map<TCallback, TaskDescriptor> mDescriptors;

    static {
        TASK_REF_COUNTER = Integer.MIN_VALUE;
    }

    public AndroidLeakManager(Class<TCallback> pCallbackClass, LeakManagerConfig pConfig) {
        super();

        mCallbackClass = pCallbackClass;
        mDefaultScheduler = new AndroidUITaskScheduler();
        mConfig = pConfig;
        mLockingStrategy = new UIThreadLockingStrategy();
        mLockingStrategy.createManager(this);
        mContainers = Collections.newSetFromMap(new ConcurrentHashMap<TaskContainer, Boolean>(DEFAULT_CAPACITY));
        mEmitters = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
        mDescriptors = new AutoCleanMap<>(DEFAULT_CAPACITY);
    }

    @Override
    public void manage(Object pEmitter) {
        if (pEmitter == null) throw new NullPointerException("Emitter is null");
        mLockingStrategy.checkCallIsAllowed();

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
        mLockingStrategy.checkCallIsAllowed();

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
        mLockingStrategy.checkCallIsAllowed();

        // Create a container to run the task.
        TaskContainer lContainer = new TaskContainer(pCallback, mDefaultScheduler);
        // Save the task before running it.
        // Note that it is safe to add the task to the container since it is an empty stub that shouldn't create any side-effect.
        if (mContainers.add(lContainer)) {
            // Prepare the task (i.e. initialize and cache needed values) after adding it because prepareToRun() is a bit
            // expensive and should be performed only if necessary.
            try {
                TaskRef lTaskRef = lContainer.prepareToRun(pCallback);
                return lContainer;
            }
            // If preparation operation fails, try to leave the manager in a consistent state without memory leaks.
            catch (RuntimeException eRuntimeException) {
                mContainers.remove(lContainer);
                throw eRuntimeException;
            }
        }
        // If an identical task is already executing, do nothing.
        else {
            return null;
        }
    }

    /**
     * Called internally when initializing a TaskDescriptor to a reference to an emitter, either managed or not. If the emitter is
     * not managed, then return an unmanaged reference (i.e. that is not stored in mEmitters).
     *
     * @param pEmitter Emitter to find the reference of.
     * @return Emitter reference. No null is returned.
     */
    protected TaskEmitterRef resolveRef(Object pEmitter) {
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
    /*private*/ class TaskContainer implements LeakContainer {
        // Handlers
        private TCallback mTask;

        // Container info.
        private volatile TaskDescriptor mDescriptor;
        private final TaskRef mTaskRef;
        private final TaskScheduler mScheduler;

        public TaskContainer(TCallback pTask, TaskScheduler pScheduler) {
            super();
            mTask = pTask;

            mDescriptor = null;
            mTaskRef = new TaskRef(TASK_REF_COUNTER++);
            mScheduler = pScheduler;
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
         * Initialize the container before running it.
         */
        protected TaskRef prepareToRun(TCallback pTaskResult) {
            // Initialize the descriptor safely in its corner and dereference required values.
            final TaskDescriptor lDescriptor = new TaskDescriptor(pTaskResult);
            if (!lDescriptor.needDereferencing(mTask)) {
                prepareTask();
            }
            // Make the descriptor visible once fully initialized.
            mDescriptor = lDescriptor;

            // Save the descriptor so that any child task can use current descriptor as a parent.
            mDescriptors.put(pTaskResult, lDescriptor); // TODO Global lock that could lead to contention. Check for optim.
            return mTaskRef;
        }

        /**
         * Dereference the task itself if it is disjoint from its handlers. This is definitive. No code inside the task is allowed
         * to access this$x references.
         */
        private void prepareTask() {
            try {
                Class<?> lTaskClass = mTask.getClass();
                while (lTaskClass != Object.class) {
                    // If current class is an inner class...
                    if ((lTaskClass.getEnclosingClass() != null) && !Modifier.isStatic(lTaskClass.getModifiers())) {
                        if (!mConfig.allowInnerTasks()) throw innerTasksNotAllowed(mTask);

                        // Remove any references to the outer class.
                        for (Field lField : lTaskClass.getDeclaredFields()) {
                            if (lField.getName().startsWith("this$")) {
                                lField.setAccessible(true);
                                lField.set(mTask, null);
                                // There should be only one outer reference per "class" in the Task class hierarchy. So we can
                                // stop as soon as the field is found as there won't be another.
                                break;
                            }
                        }
                    }
                    lTaskClass = lTaskClass.getSuperclass();
                }
            } catch (IllegalAccessException | IllegalArgumentException exception) {
                throw internalError(exception);
            }
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) return true;
            if (pOther == null) return false;
            if (getClass() != pOther.getClass()) return false;

            @SuppressWarnings("unchecked")
            TaskContainer lOtherContainer = (TaskContainer) pOther;
            // If the task has no Id, then we use task equality method. This is likely to turn into a simple reference check.
            if (mTask != null) return mTask.equals(lOtherContainer.mTask);
            // A container can't be created with a null task. So the following case should never occur.
            else throw internalError();
        }

        @Override
        public int hashCode() {
            if (mTask != null) {
                return mTask.hashCode();
            } else {
                throw internalError();
            }
        }
    }

    /**
     * Contain all the information necessary to restore all the emitters (even parent emitters) of a task. Once prepareToRun() is
     * called, the content of this class is not modified anymore (except the emitter and the reference counter dedicated to
     * referencing and dereferencing).
     */
    private class TaskDescriptor {
        private final TCallback mTaskResult;
        private List<TaskEmitterDescriptor> mEmitterDescriptors; // Never modified once initialized in prepareDescriptor().
        private List<TaskDescriptor> mParentDescriptors; // Never modified once initialized in prepareDescriptor().
        // Counts the number of time a task has been referenced without being dereferenced. A task will be dereferenced only when
        // this counter reaches 0, which means that no other task needs references to be set. This situation can occur for example
        // when starting a child task from a parent task handler (e.g. in onFinish()): when the child task is launched, it must
        // not dereference emitters because the parent task is still in its onFinish() handler and may need references to them.
        private int mReferenceCounter;
        private final Lock mLock;

        // TODO Boolean option to indicate if we should look for emitter or if task is not "managed".
        public TaskDescriptor(TCallback pTaskResult) {
            mTaskResult = pTaskResult;
            mEmitterDescriptors = null;
            mParentDescriptors = null;
            mReferenceCounter = 0;
            mLock = mLockingStrategy.createLock();

            prepareDescriptor();
        }

        public boolean needDereferencing(Object pTask) {
            return pTask == mTaskResult;
        }

        public boolean usesEmitter(TaskEmitterId pEmitterId) {
            if (mEmitterDescriptors != null) {
                for (TaskEmitterDescriptor lEmitterDescriptor : mEmitterDescriptors) {
                    if (lEmitterDescriptor.usesEmitter(pEmitterId)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Locate all the outer object references (e.g. this$0) inside the task class, manage them if necessary and cache emitter
         * field properties for later use. Check is performed recursively on all super classes too.
         */
        private void prepareDescriptor() {
            try {
                // Go through the main class and each of its super classes and look for "this$" fields.
                Class<?> lTaskResultClass = mTaskResult.getClass();
                while (lTaskResultClass != Object.class) {
                    // If current class is an inner class...
                    if ((lTaskResultClass.getEnclosingClass() != null) && !Modifier.isStatic(lTaskResultClass.getModifiers())) {
                        // Find emitter references and generate a descriptor from them.
                        for (Field lField : lTaskResultClass.getDeclaredFields()) {
                            if (lField.getName().startsWith("this$")) {
                                prepareEmitterField(lField);
                                // There should be only one outer reference per "class" in the Task class hierarchy. So we can
                                // stop as soon as the field is found as there won't be another.
                                break;
                            }
                        }
                    }
                    lTaskResultClass = lTaskResultClass.getSuperclass();
                }
            } finally {
                if (mEmitterDescriptors != null) {
                    for (TaskEmitterDescriptor lEmitterDescriptor : mEmitterDescriptors) {
                        lEmitterDescriptor.dereference(mTaskResult);
                    }
                }
            }
        }

        /**
         * Find and save the descriptor of the corresponding field, i.e. an indirect (weak) reference pointing to the emitter
         * through its Id or a simple indirect (weak) reference for unmanaged emitters.
         * 
         * @param pField Field to manage.
         */
        private void prepareEmitterField(Field pField) {
            try {
                pField.setAccessible(true);

                // Extract the emitter "reflectively" and compute its Id.
                TaskEmitterRef lEmitterRef;
                Object lEmitter = pField.get(mTaskResult);

                if (lEmitter != null) {
                    lEmitterRef = resolveRef(lEmitter);
                    lookForParentDescriptor(pField, lEmitter);
                }
                // If reference is null, that means the emitter is probably used in a parent container and already managed.
                // Try to find its Id in parent containers.
                else {
                    // Not sure there is a problem here. The list of parent descriptors should be entirely created before to be
                    // sure we can properly resolve reference. However this$x fields are processed from child class to super
                    // classes. My guess is that top-most child class will always have its outer reference filled, which itself
                    // will point to parent outer objects. And if one of the outer reference is created explicitly through a
                    // "myOuter.new Inner()", well the outer class reference myOuter cannot be null or a NullPointerException is
                    // thrown by the Java language anyway. But that remains late-night suppositions... Anyway if it doesn't work
                    // it probably means you're just writing really bad code so just stop it please! Note that this whole case can
                    // occur only when onFinish() is called with keepResultOnHold option set to false (in which case referencing
                    // is not guaranteed be fully applied).
                    lEmitterRef = resolveRefInParentDescriptors(pField);
                }

                if (lEmitterRef != null) {
                    if (mEmitterDescriptors == null) {
                        // Most of the time, a task will have only one emitter. Hence a capacity of 1.
                        mEmitterDescriptors = new ArrayList<>(1);
                    }
                    mEmitterDescriptors.add(new TaskEmitterDescriptor(pField, lEmitterRef));
                } else {
                    // Maybe this is too brutal and we should do nothing, hoping that no access will be made. But for the moment I
                    // really think this case should never happen under normal conditions. See the big paragraph above...
                    throw emitterIdCouldNotBeDetermined(mTaskResult);
                }
            } catch (IllegalAccessException | IllegalArgumentException exception) {
                throw internalError(exception);
            }
        }

        /**
         * Sometimes, we cannot resolve a parent emitter reference because it has already been dereferenced. In that case, we
         * should find the emitter reference somewhere in parent descriptors.
         * 
         * @param pField Emitter field.
         * @return The emitter if it could be found or null else.
         */
        private TaskEmitterRef resolveRefInParentDescriptors(Field pField) {
            if (mParentDescriptors != null) {
                for (TaskDescriptor lParentDescriptor : mParentDescriptors) {
                    TaskEmitterRef lEmitterRef;
                    if (mEmitterDescriptors != null) {
                        for (TaskEmitterDescriptor lParentEmitterDescriptor : lParentDescriptor.mEmitterDescriptors) {
                            // We have found the right ref if its field has the same type than the field of the emitter we look
                            // for.
                            // I turned my mind upside-down but this seems to work.
                            lEmitterRef = lParentEmitterDescriptor.hasSameType(pField);
                            if (lEmitterRef != null) return lEmitterRef;
                        }
                    }

                    lEmitterRef = lParentDescriptor.resolveRefInParentDescriptors(pField);
                    if (lEmitterRef != null) return lEmitterRef;
                }
            }
            return null;
        }

        /**
         * Check for parent tasks (i.e. a task containing directly or indirectly innertasks) and their descriptors that will be
         * necessary to restore all emitters of a task.
         * 
         * @param pField Emitter field.
         * @param pEmitter Effective emitter reference. Must not be null.
         */
        private void lookForParentDescriptor(Field pField, Object pEmitter) {
            if (mCallbackClass.isAssignableFrom(pField.getType())) {
                @SuppressWarnings("SuspiciousMethodCalls")
                TaskDescriptor lDescriptor = mDescriptors.get(pEmitter);
                if (lDescriptor == null) throw taskExecutedFromUnexecutedTask(pEmitter);

                if (mParentDescriptors == null) {
                    // A task will have most of the time no parents. Hence lazy-initialization. But if that's not the case, then a
                    // task will usually have only one parent, rarely more. Hence a capacity of 1.
                    mParentDescriptors = new ArrayList<>(1);
                }
                mParentDescriptors.add(lDescriptor);
            } else {
                try {
                    // Go through the main class and each of its super classes and look for "this$" fields.
                    Class<?> lTaskResultClass = pEmitter.getClass();
                    while (lTaskResultClass != Object.class) {
                        // If current class is an inner class...
                        if ((lTaskResultClass.getEnclosingClass() != null) && !Modifier.isStatic(lTaskResultClass.getModifiers())) {
                            // Find all parent emitter references and their corresponding descriptors.
                            for (Field lField : lTaskResultClass.getDeclaredFields()) {
                                if (lField.getName().startsWith("this$")) {
                                    lField.setAccessible(true);
                                    Object lParentEmitter = lField.get(pEmitter);
                                    if (lParentEmitter != null) {
                                        lookForParentDescriptor(lField, lParentEmitter);
                                    }
                                    // else {
                                    //     Look for the big comment in prepareEmitterField(). Here we try to check the whole
                                    //     hierarchy of parent this$x to look for parent descriptors (not only this$x for the
                                    //     handler class and its super classes). In this case, if we get a null, I really think we
                                    //     are stuck if there is a Task handler and its associated descriptor hidden deeper behind
                                    //     this null reference. Basically we can do nothing against this except maybe a warning as
                                    //     code may still be correct if the null reference just hides e.g. a managed object (e.g.
                                    //     an Activity). That's why an exception would be too brutal. User will get a
                                    //     NullPointerException anyway if he try to go through such a reference. Again note that
                                    //     this whole case can occur only when onFinish() is called with keepResultOnHold option
                                    //     set to false (in which case referencing is not guaranteed be fully applied).
                                    // }

                                    // There should be only one outer reference per "class" in the Task class hierarchy. So we can
                                    // stop as soon as the field is found as there won't be another.
                                    break;
                                }
                            }
                        }
                        lTaskResultClass = lTaskResultClass.getSuperclass();
                    }
                } catch (IllegalArgumentException | IllegalAccessException exception) {
                    throw internalError(exception);
                }
            }
        }

        /**
         * Restore all the emitters back into the task handler. Called before each task handler is executed to avoid
         * NullPointerException when accessing outer emitters. Referencing can fail if an emitter has been unmanaged. In that
         * case, any set reference is rolled-back and dereferenceEmitter() shouldn't be called. But if referencing succeeds, then
         * dereferenceEmitter() MUST be called eventually (preferably using a finally block).
         * 
         * @param pRollbackOnFailure True to cancel referencing if one of the emitter cannot be restored, or false if partial
         *            referencing is allowed.
         * @return True if restoration was performed properly. This may be false if a previously managed object become unmanaged
         *         meanwhile.
         */
        protected/*private*/ boolean referenceEmitter(boolean pRollbackOnFailure) {
            // Try to restore emitters in parent containers first. Everything is rolled-back if referencing fails.
            if (mParentDescriptors != null) {
                for (TaskDescriptor lParentDescriptor : mParentDescriptors) {
                    if (!lParentDescriptor.referenceEmitter(pRollbackOnFailure)) return false;
                }
            }

            // Restore references for current container if referencing succeeded previously.
            if (mEmitterDescriptors != null) {
                mLock.lock();
                try {
                    // TODO There is a race problem in this code. A TaskEmitterRef can be used several times for one
                    // TaskDescriptor because of parent or superclass emitters ref that may be identical. In that case, a call
                    // to manage() on another thread during referenceEmitter() may cause two different emitters to be restored
                    // whereas we would expect the same ref.
                    if ((mReferenceCounter++) == 0) {
                        for (TaskEmitterDescriptor lEmitterDescriptor : mEmitterDescriptors) {
                            if (!lEmitterDescriptor.reference(mTaskResult) && pRollbackOnFailure) {
                                // Rollback modifications in case of failure.
                                --mReferenceCounter;
                                for (TaskEmitterDescriptor lRolledEmitterDescriptor : mEmitterDescriptors) {
                                    if (lRolledEmitterDescriptor == lEmitterDescriptor) break;
                                    lRolledEmitterDescriptor.dereference(mTaskResult);
                                }
                                return false;
                            }
                        }
                    }
                }
                // Note: Rollback any modifications if an exception occurs. Having an exception here denotes an internal bug.
                catch (AndroidLeakManagerException eLeakManagerAndroidException) {
                    --mReferenceCounter;
                    // Note that if referencing failed at some point, dereferencing is likely to fail too. That's not a big
                    // issue since an exception will be thrown in both cases anyway.
                    for (TaskEmitterDescriptor lRolledEmitterDescriptor : mEmitterDescriptors) {
                        lRolledEmitterDescriptor.dereference(mTaskResult);
                    }
                    throw eLeakManagerAndroidException;
                } finally {
                    mLock.unlock();
                }
            }
            return true;
        }

        /**
         * Remove emitter references from the task handler. Called after each task handler is executed to avoid memory leaks.
         */
        protected/*private*/ void dereferenceEmitter() {
            // Try to dereference emitters in parent containers first.
            if (mParentDescriptors != null) {
                for (TaskDescriptor lParentDescriptor : mParentDescriptors) {
                    lParentDescriptor.dereferenceEmitter();
                }
            }

            if (mEmitterDescriptors != null) {
                mLock.lock();
                try {
                    // Note: No need to rollback modifications if an exception occur. Leave references as is, thus creating a
                    // memory leak. We can't do much about it since having an exception here denotes an internal bug.
                    if ((--mReferenceCounter) == 0) {
                        for (TaskEmitterDescriptor lEmitterDescriptor : mEmitterDescriptors) {
                            lEmitterDescriptor.dereference(mTaskResult);
                        }
                    }
                } finally {
                    mLock.unlock();
                }
            }
        }
    }

    /**
     * Contains all the information necessary to restore a single emitter on a task handler (its field and its generated Id).
     */
    private final class TaskEmitterDescriptor {
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
        public boolean reference(TCallback pTaskResult) {
            try {
                Object lEmitter = mEmitterRef.get();
                if (lEmitter != null) {
                    mEmitterField.set(pTaskResult, lEmitter);
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
        public void dereference(TCallback pTaskResult) {
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

    /**
     * Represents a reference to an emitter. Its goal is to add a level of indirection to the emitter so that several tasks can
     * easily share updates made to an emitter.
     */
    private static final class TaskEmitterRef {
        private final TaskEmitterId mEmitterId;
        private volatile WeakReference<Object> mEmitterRef;

        public TaskEmitterRef(Object pEmitterValue) {
            mEmitterId = null;
            set(pEmitterValue);
        }

        public TaskEmitterRef(TaskEmitterId pEmitterId, Object pEmitterValue) {
            mEmitterId = pEmitterId;
            set(pEmitterValue);
        }

        public boolean hasSameId(TaskEmitterId pTaskEmitterId) {
            return (mEmitterId != null) && mEmitterId.equals(pTaskEmitterId);
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

            TaskEmitterRef lOther = (TaskEmitterRef) pOther;
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

    /**
     * Contains the information to store the Id of an emitter. Emitter class is necessary since if internal Ids may be quite
     * common and thus similar between emitters of different types (e.g. fragments which have integer Ids starting from 0).
     */
    private static final class TaskEmitterId {
        private final Class<?> mType;
        private final Object mId;

        public TaskEmitterId(Class<?> pType, Object pId) {
            super();
            mType = pType;
            mId = pId;
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) return true;
            if (pOther == null) return false;
            if (getClass() != pOther.getClass()) return false;

            TaskEmitterId lOther = (TaskEmitterId) pOther;
            if (mId == null) {
                if (lOther.mId != null) return false;
            } else if (!mId.equals(lOther.mId)) return false;

            if (mType == null) return lOther.mType == null;
            else return mType.equals(lOther.mType);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mId == null) ? 0 : mId.hashCode());
            result = prime * result + ((mType == null) ? 0 : mType.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "TaskEmitterId [mType=" + mType + ", mId=" + mId + "]";
        }
    }

    /**
     * Allows changing the way things are synchronized in the code (i.e. everything on the UI-Thread or multi-threaded).
     */
    public interface LockingStrategy {
        void createManager(AndroidLeakManager pAndroidLeakManager);

        Lock createLock();

        void checkCallIsAllowed();
    }

    /**
     * Everything is done on the UI-Thread. No lock required.
     */
    public class UIThreadLockingStrategy implements LockingStrategy {
        private Looper mUILooper;

        public UIThreadLockingStrategy() {
            super();
            mUILooper = Looper.getMainLooper();
        }

        @Override
        public void createManager(AndroidLeakManager pAndroidLeakManager) {
            pAndroidLeakManager.mContainers = Collections.newSetFromMap(new ConcurrentHashMap<TaskContainer, Boolean>(DEFAULT_CAPACITY));
            pAndroidLeakManager.mEmitters = new ConcurrentHashMap<TaskEmitterId, TaskEmitterRef>(DEFAULT_CAPACITY);
            pAndroidLeakManager.mDescriptors = new AutoCleanMap<>(DEFAULT_CAPACITY);
        }

        @Override
        public Lock createLock() {
            return new EmptyLock();
        }

        @Override
        public void checkCallIsAllowed() {
            if (Looper.myLooper() != mUILooper) throw mustBeExecutedFromUIThread();
        }
    }

    /**
     * Tasks and handlers can be executed on any threads concurrently.
     */
    public class MultiThreadLockingStrategy implements LockingStrategy {
        @Override
        public void createManager(AndroidLeakManager pAndroidLeakManager) {
            pAndroidLeakManager.mContainers = Collections.newSetFromMap(new ConcurrentHashMap<TaskContainer, Boolean>(DEFAULT_CAPACITY));
            pAndroidLeakManager.mEmitters = new ConcurrentHashMap<TaskEmitterId, TaskEmitterRef>(DEFAULT_CAPACITY);
            pAndroidLeakManager.mDescriptors = new AutoCleanMap<TCallback, TaskDescriptor>(DEFAULT_CAPACITY);
        }

        @Override
        public Lock createLock() {
            return new ReentrantLock();
        }

        @Override
        public void checkCallIsAllowed() {
        }
    }
}