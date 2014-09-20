package com.codexperiments.leakeeper;

import com.codexperiments.leakeeper.config.enforcer.NoThreadEnforcer;
import com.codexperiments.leakeeper.config.enforcer.ThreadEnforcer;
import com.codexperiments.leakeeper.config.factory.LockFactory;
import com.codexperiments.leakeeper.config.factory.MultiThreadLockFactory;
import com.codexperiments.leakeeper.config.factory.SingleThreadLockFactory;
import com.codexperiments.leakeeper.config.resolver.EmitterResolver;
import com.codexperiments.leakeeper.internal.AutoCleanMap;
import com.codexperiments.leakeeper.internal.EmitterId;
import com.codexperiments.leakeeper.internal.EmitterRef;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.codexperiments.leakeeper.CallbackException.*;

/**
 * Terminology:
 * <p/>
 * <ul> <li>Emitter: A task emitter is, in Java terms, an outer class object that requests a task to execute. Thus, a task can
 * have emitters only if it is an inner, local or anonymous class. It's important to note that an object can have one or several
 * emitters since this is allowed by the Java language (an inner class can keep reference to several enclosing class).</li>
 * <li>Dereferencing: An inner class task keeps references to its emitters. These references must be removed temporarily during
 * processing to avoid possible memory leaks (e.g. if a task references an activity that gets destroyed during processing).</li>
 * <li>Referencing: References to emitters must be restored to execute task handlers (onFinish(), onFail(), onProgress()) or else,
 * the task would be unable to communicate with the outside world since it has be dereferenced. Referencing is possible only if
 * all the necessary emitters, managed by the CallbackManager, are still reachable. If not, task handlers cannot be executed until all
 * are reachable (and if configuration requires to keep results on hold).</li> </ul>
 * <p/>
 * <b>The problem:</b>
 * <p/>
 * There are many ways to handle asynchronous tasks in Android to load data or perform some background processing. Several ways to
 * handle this exist, among which: <ul> <li>AsyncTasks: One of the most efficient ways to write asynchronous tasks but also to
 * make mistakes. Can be used as such mainly for short-lived tasks (or by using WeakReferences).</li> <li>Services or
 * IntentServices (with Receivers): Probably the most flexible and safest way to handle asynchronous tasks, but requires some
 * boilerplate "plumbing"</li> <li>Loaders: which are tied to the activity life-cycle and also a bit difficult to write when
 * handling all the specific cases that may occur (Loader reseted, etc.). They require less plumbing but still some.</li>
 * <li>Content Providers: which are just nice to use to create a remote data source. Cumbersome and annoying to write for any
 * other use... And they are not inherently threaded anyway.</li> <li>...</li> </ul> Each technique has its drawbacks. The most
 * practical way, AsyncTasks, can easily cause memory leaks which occur especially with inner classes which keep a reference to
 * the outer object. A typical example is an Activity referenced from an inner AsyncTask: when the Activity is destroyed because
 * of a configuration change (e.g. screen rotation) or because user leave the Activity (e.g. with Home button), then the executing
 * AsyncTask still references its containing Activity which cannot be garbage collected. Even worse, accessing the emitting
 * Activity after AsyncTask is over may cause either no result at all or exceptions, because a new version of the Activity may
 * have been created in-between and the older one is not displayed any more or has freed some resources.
 * <p/>
 * <b>How it works:</b>
 * <p/>
 * As soon as a task is enqueued in execute(), all its emitters are dereferenced to avoid any possible memory leaks during
 * processing (in Task.onProcess()). In other words, any emitters (i.e. outer class references) are replaced with null. This means
 * that your Task: <ul> <li>Can execute safely without memory leaks. Activity or any other emitter can still be garbage
 * collected.</li> <li><b>CANNOT access outer emitters (again, any outer class reference) from the onProcess method() or must use
 * a static Task!</b> That's price to pay for this memory safety... Use allowInnerTasks() in Configuration object to forbid the
 * use of inner tasks.</li> <li><b>Any member variables need by a Task must be copied in Task constructor.</b> That way, the Task
 * can work safely in a closed environment without the interference of other threads. Indeed, don't share any variable between
 * onProcess() and any other threads, UI-Thread included, as this could lead to unpredictable result (because of Thread caching or
 * instruction reordering) unless some synchronization is performed (which can lead to bottleneck or a dead lock in extreme case
 * if not appropriately handled).</li> </ul>
 * <p/>
 * Before, during or after processing, several handlers (i.e. callbacks) can be called: <ul> <li>onStart()</li>
 * <li>onProgress()</li> <li>onFinish()</li> <li>onFail()</li> </ul> Right before and after these handlers are invoked, emitters
 * are respectively referenced and dereferenced to allow accessing the outer class. If outer class is not available (e.g. if
 * Activity has been destroyed but not recreated yet).
 * <p/>
 * TODO Rename manage => manageEmitter and wrap => wrapCallback?
 * <p/>
 * TODO Remove TaskId but create a TaskEquality helper class.
 * <p/>
 * TODO Handle cancellation.
 * <p/>
 * TODO onBeforeProcess / onRestore / onCommit
 * <p/>
 * TODO Save TaskRefs list.
 * <p/>
 * TODO TaskRef add a Tag
 * <p/>
 * TODO pending(TaskType)
 * <p/>
 * TODO Rebind
 */
public class CallbackManager<TCallback> {
    private static final int DEFAULT_CAPACITY = 64;

    public static <TCallback> CallbackManager<TCallback> singleThreaded(Class<TCallback> pCallbackClass, EmitterResolver pEmitterResolver) {
        Set<CallbackDescriptor<TCallback>> containers = new HashSet<>(DEFAULT_CAPACITY);
        Map<EmitterId, EmitterRef> emitters = new HashMap<>(DEFAULT_CAPACITY);
        Map<TCallback, CallbackDescriptor<TCallback>> descriptors = AutoCleanMap.create(DEFAULT_CAPACITY);
        LockFactory lockFactory = new SingleThreadLockFactory();

        // TODO boolean android = true; android ? new AndroidUIThreadEnforcer() :
        ThreadEnforcer threadEnforcer = new NoThreadEnforcer();

        return new CallbackManager<>(pCallbackClass, lockFactory, threadEnforcer, pEmitterResolver, containers, emitters, descriptors);
    }

    public static <TCallback> CallbackManager<TCallback> multiThreaded(Class<TCallback> pCallbackClass, EmitterResolver pEmitterResolver) {
        Set<CallbackDescriptor<TCallback>> containers = Collections.newSetFromMap(new ConcurrentHashMap<CallbackDescriptor<TCallback>, Boolean>(DEFAULT_CAPACITY));
        Map<EmitterId, EmitterRef> emitters = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
        Map<TCallback, CallbackDescriptor<TCallback>> descriptors = AutoCleanMap.create(DEFAULT_CAPACITY);
        LockFactory lockFactory = new MultiThreadLockFactory();
        ThreadEnforcer threadEnforcer = new NoThreadEnforcer();

        return new CallbackManager<>(pCallbackClass, lockFactory, threadEnforcer, pEmitterResolver, containers, emitters, descriptors);
    }


    private final Class<TCallback> mCallbackClass;
    private final LockFactory mLockFactory;
    private final ThreadEnforcer mThreadEnforcer;
    private final EmitterResolver mEmitterResolver;

    // Keep tracks of all emitters. Note that TaskEmitterRef uses a weak reference to avoid memory leaks. This Map is never
    // cleaned and accumulates references because it assumes that any object that managed object set doesn't grow infinitely but
    // is rather limited (e.g. typically all fragments, activity and manager in an Application).
    private final Map<EmitterId, EmitterRef> mEmitters;
    // Allow getting back an existing descriptor through its handler when dealing with nested tasks. An AutoCleanMap is necessary
    // since there is no way to know when a handler are not necessary anymore.
    /*private*/ final Map<TCallback, CallbackDescriptor<TCallback>> mDescriptors; // TODO Handle WeakRef removal this with a kind of counter in descriptor?


    protected CallbackManager(Class<TCallback> pCallbackClass, LockFactory pLockFactory, ThreadEnforcer pThreadEnforcer,
                              EmitterResolver pEmitterResolver, Set<CallbackDescriptor<TCallback>> pContainers,
                              Map<EmitterId, EmitterRef> pEmitters, Map<TCallback, CallbackDescriptor<TCallback>> pDescriptors) {
        super();

        mCallbackClass = pCallbackClass;
        mLockFactory = pLockFactory;
        mThreadEnforcer = pThreadEnforcer;
        mEmitterResolver = pEmitterResolver;

        mEmitters = pEmitters;
        mDescriptors = pDescriptors;
    }

    public void manage(Object pEmitter) {
        if (pEmitter == null) throw new NullPointerException("Emitter is null");
        mThreadEnforcer.enforce();

        // Save the new emitter in the reference list. Replace the existing one, if any, according to its id (the old one is
        // considered obsolete). Emitter Id is computed by the configuration and can be null if emitter is not managed.
        Object lEmitterIdValue = mEmitterResolver.resolveEmitterId(pEmitter);
        // Emitter id must not be the emitter itself or we have a leak. Warn user about this (tempting) configuration misuse.
        if ((lEmitterIdValue == null) || (lEmitterIdValue == pEmitter)) throw invalidEmitterId(lEmitterIdValue, pEmitter);

        // Save the reference of the emitter. Initialize it lazily if it doesn't exist.
        EmitterId lEmitterId = new EmitterId(pEmitter.getClass(), lEmitterIdValue);
        EmitterRef lEmitterRef = mEmitters.get(lEmitterId);
        if (lEmitterRef == null) {
            /*lEmitterRef =*/
            mEmitters.put(lEmitterId, new EmitterRef(lEmitterId, pEmitter));
        } else {
            lEmitterRef.set(pEmitter);
        }
    }

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
        Object lEmitterIdValue = mEmitterResolver.resolveEmitterId(pEmitter);
        if (lEmitterIdValue != null) {
            EmitterId lEmitterId = new EmitterId(pEmitter.getClass(), lEmitterIdValue);
            EmitterRef lEmitterRef = mEmitters.get(lEmitterId);
            if ((lEmitterRef != null) && (lEmitterRef.get() == pEmitter)) {
                lEmitterRef.clear();
            }
        }
    }

    public CallbackDescriptor<TCallback> wrap(TCallback pCallback) {
        if (pCallback == null) throw new NullPointerException("Callback is null");
        mThreadEnforcer.enforce();

        // Create a container to run the task.
        // Prepare the task (i.e. initialize and cache needed values) after adding it because prepareToRun() is a bit
        // expensive and should be performed only if necessary.
        final CallbackDescriptor<TCallback> lDescriptor = new CallbackDescriptor<TCallback>(this, pCallback, mLockFactory.create());
        // Save the descriptor so that any child task can use current descriptor as a parent.
        mDescriptors.put(pCallback, lDescriptor);
        return lDescriptor;
    }

    /**
     * Called internally when initializing a TaskDescriptor to a reference to an emitter, either managed or not. If the emitter is
     * not managed, then return an unmanaged reference (i.e. that is not stored in mEmitters).
     *
     * @param pEmitter Emitter to find the reference of.
     * @return Emitter reference. No null is returned.
     */
    EmitterRef resolveEmitter(Object pEmitter) {
        // Save the new emitter in the reference list. Replace the existing one, if any, according to its id (the old one is
        // considered obsolete). Emitter Id is computed by the configuration strategy. Note that an emitter Id can be null if no
        // dereferencing should be performed.
        Object lEmitterIdValue = mEmitterResolver.resolveEmitterId(pEmitter);
        // Emitter id must not be the emitter itself or we have a leak. Warn user about this (tempting) configuration misuse.
        // Note that when we arrive here, pEmitter can't be null.
        if (lEmitterIdValue == pEmitter) throw invalidEmitterId(lEmitterIdValue, pEmitter);

        EmitterRef lEmitterRef;
        // Managed emitter case.
        if (lEmitterIdValue != null) {
            EmitterId lEmitterId = new EmitterId(pEmitter.getClass(), lEmitterIdValue);
            lEmitterRef = mEmitters.get(lEmitterId);
            // If emitter is managed by the user explicitly and is properly registered in the emitter list, do nothing. User can
            // update reference himself through manage(Object) later. But if emitter is managed (i.e. emitter Id returned by
            // configuration is not null) but is not in the emitter list, then a call to manage() is missing. Warn the user.
            if (lEmitterRef == null) throw emitterNotManaged(lEmitterIdValue, pEmitter);
        }
        // Unmanaged emitter case.
        else {
            // TODO The EmitterResolver should throw in that case? Document...
            //if (!mEmitterResolver.allowUnmanagedEmitters()) throw unmanagedEmittersNotAllowed(pEmitter);
            // TODO This is wrong! There should be only one TaskEmitterRef per emitter or concurrency problems may occur.
            lEmitterRef = new EmitterRef(pEmitter);
        }
        return lEmitterRef;
    }

    CallbackDescriptor<TCallback> resolveDescriptor(Field pField, Object pEmitter) {
        if (!mCallbackClass.isAssignableFrom(pField.getType())) return null;

        @SuppressWarnings("SuspiciousMethodCalls")
        CallbackDescriptor<TCallback> lDescriptor = mDescriptors.get(pEmitter);
        if (lDescriptor != null) return lDescriptor;
        else throw taskExecutedFromUnexecutedTask(pEmitter);
    }
}