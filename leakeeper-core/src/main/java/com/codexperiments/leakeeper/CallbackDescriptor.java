package com.codexperiments.leakeeper;

import com.codexperiments.leakeeper.internal.EmitterDescriptor;
import com.codexperiments.leakeeper.internal.EmitterId;
import com.codexperiments.leakeeper.internal.EmitterRef;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import static com.codexperiments.leakeeper.CallbackException.emitterIdCouldNotBeDetermined;
import static com.codexperiments.leakeeper.CallbackException.internalError;

/**
 * Contain all the information necessary to restore all the emitters (even parent emitters) of a task. Once prepareToRun() is
 * called, the content of this class is not modified anymore (except the emitter and the reference counter dedicated to
 * referencing and dereferencing).
 */
public final class CallbackDescriptor<TCallback> {
    private final CallbackManager mManager;
    private final Object mCallback;
    private List<EmitterDescriptor> mEmitterDescriptors; // Never modified once initialized in prepareDescriptor().
    private List<CallbackDescriptor<TCallback>> mParentDescriptors; // Never modified once initialized in prepareDescriptor().
    // Counts the number of time a task has been referenced without being dereferenced. A task will be dereferenced only when
    // this counter reaches 0, which means that no other task needs references to be set. This situation can occur for example
    // when starting a child task from a parent task handler (e.g. in onFinish()): when the child task is launched, it must
    // not dereference emitters because the parent task is still in its onFinish() handler and may need references to them.
    private int mReferenceCounter;
    private final Lock mLock;

    // TODO Boolean option to indicate if we should look for emitter or if task is not "managed".
    public CallbackDescriptor(CallbackManager pManager, Object pCallback, Lock pLock) {
        mManager = pManager;
        mCallback = pCallback;
        mEmitterDescriptors = null;
        mParentDescriptors = null;
        mReferenceCounter = 0;
        mLock = pLock;

        prepareDescriptor();
    }

    // TODO Useless?
    public boolean usesEmitter(EmitterId pEmitterId) {
        if (mEmitterDescriptors != null) {
            for (EmitterDescriptor lEmitterDescriptor : mEmitterDescriptors) {
                if (lEmitterDescriptor.usesEmitter(pEmitterId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Restore all the emitters back into the task handler. Called before each task handler is executed to avoid
     * NullPointerException when accessing outer emitters. Referencing can fail if an emitter has been unmanaged. In that case,
     * any set reference is rolled-back and dereferenceEmitter() shouldn't be called. But if referencing succeeds, then
     * dereferenceEmitter() MUST be called eventually (preferably using a finally block).
     *
     * @param pRollbackOnFailure True to cancel referencing if one of the emitter cannot be restored, or false if partial
     *                           referencing is allowed.
     * @return True if restoration was performed properly. This may be false if a previously managed object become unmanaged
     * meanwhile.
     */
    public boolean referenceEmitter(boolean pRollbackOnFailure) {
        // Try to restore emitters in parent containers first. Everything is rolled-back if referencing fails.
        if (mParentDescriptors != null) {
            for (CallbackDescriptor<TCallback> lParentDescriptor : mParentDescriptors) {
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
                    for (EmitterDescriptor lEmitterDescriptor : mEmitterDescriptors) {
                        if (!lEmitterDescriptor.reference(mCallback) && pRollbackOnFailure) {
                            // Rollback modifications in case of failure.
                            --mReferenceCounter;
                            for (EmitterDescriptor lRolledEmitterDescriptor : mEmitterDescriptors) {
                                if (lRolledEmitterDescriptor == lEmitterDescriptor) break;
                                lRolledEmitterDescriptor.dereference(mCallback);
                            }
                            return false;
                        }
                    }
                }
            }
            // Note: Rollback any modifications if an exception occurs. Having an exception here denotes an internal bug.
            catch (CallbackException eLeakManagerAndroidException) {
                --mReferenceCounter;
                // Note that if referencing failed at some point, dereferencing is likely to fail too. That's not a big
                // issue since an exception will be thrown in both cases anyway.
                for (EmitterDescriptor lRolledEmitterDescriptor : mEmitterDescriptors) {
                    lRolledEmitterDescriptor.dereference(mCallback);
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
    public void dereferenceEmitter() {
        // Try to dereference emitters in parent containers first.
        if (mParentDescriptors != null) {
            for (CallbackDescriptor<TCallback> lParentDescriptor : mParentDescriptors) {
                lParentDescriptor.dereferenceEmitter();
            }
        }

        if (mEmitterDescriptors != null) {
            mLock.lock();
            try {
                // Note: No need to rollback modifications if an exception occur. Leave references as is, thus creating a
                // memory leak. We can't do much about it since having an exception here denotes an internal bug.
                if ((--mReferenceCounter) == 0) {
                    for (EmitterDescriptor lEmitterDescriptor : mEmitterDescriptors) {
                        lEmitterDescriptor.dereference(mCallback);
                    }
                }
            } finally {
                mLock.unlock();
            }
        }
    }

    /**
     * Locate all the outer object references (e.g. this$0) inside the task class, manage them if necessary and cache emitter
     * field properties for later use. Check is performed recursively on all super classes too.
     */
    private void prepareDescriptor() {
        try {
            // Go through the main class and each of its super classes and look for "this$" fields.
            Class<?> lTaskResultClass = mCallback.getClass();
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
                for (EmitterDescriptor lEmitterDescriptor : mEmitterDescriptors) {
                    lEmitterDescriptor.dereference(mCallback);
                }
            }
        }
    }

    /**
     * Find and save the descriptor of the corresponding field, i.e. an indirect (weak) reference pointing to the emitter through
     * its Id or a simple indirect (weak) reference for unmanaged emitters.
     *
     * @param pField Field to manage.
     */
    private void prepareEmitterField(Field pField) {
        try {
            pField.setAccessible(true);

            // Extract the emitter "reflectively" and compute its Id.
            EmitterRef lEmitterRef;
            Object lEmitter = pField.get(mCallback);

            if (lEmitter != null) {
                lEmitterRef = mManager.resolveEmitter(lEmitter);
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
                mEmitterDescriptors.add(new EmitterDescriptor(pField, lEmitterRef));
            } else {
                // Maybe this is too brutal and we should do nothing, hoping that no access will be made. But for the moment I
                // really think this case should never happen under normal conditions. See the big paragraph above...
                throw emitterIdCouldNotBeDetermined(mCallback);
            }
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            throw internalError(exception);
        }
    }

    /**
     * Sometimes, we cannot resolve a parent emitter reference because it has already been dereferenced. In that case, we should
     * find the emitter reference somewhere in parent descriptors.
     *
     * @param pField Emitter field.
     * @return The emitter if it could be found or null else.
     */
    private EmitterRef resolveRefInParentDescriptors(Field pField) {
        if (mParentDescriptors != null) {
            for (CallbackDescriptor<TCallback> lParentDescriptor : mParentDescriptors) {
                EmitterRef lEmitterRef;
                if (mEmitterDescriptors != null) {
                    for (EmitterDescriptor lParentEmitterDescriptor : lParentDescriptor.mEmitterDescriptors) {
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
     * @param pField   Emitter field.
     * @param pEmitter Effective emitter reference. Must not be null.
     */
    private void lookForParentDescriptor(Field pField, Object pEmitter) {
        CallbackDescriptor<TCallback> lDescriptor = mManager.resolveDescriptor(pField, pEmitter);
        if (lDescriptor != null) {
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
}