package com.codexperiments.leakeeper.internal;

//import android.annotation.TargetApi;
//import android.os.Build;
//import android.os.Process;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO Comments
 */
public class AutoCleanMap<TKey, TValue> extends AbstractMap<TKey, TValue> {
    private ConcurrentHashMap<WeakKey<TKey>, WeakValue<TValue>> mMap;
    private ReferenceQueue<TKey> mQueue;

    public static <TKey, TValue> AutoCleanMap<TKey, TValue> create(int pCapacity) {
        AutoCleanMap<TKey, TValue> autoCleanMap = new AutoCleanMap<>(pCapacity);
        autoCleanMap.startCleaning();
        return autoCleanMap;
    }

    protected AutoCleanMap(int pCapacity) {
        mMap = new ConcurrentHashMap<WeakKey<TKey>, WeakValue<TValue>>(pCapacity);
        mQueue = new ReferenceQueue<TKey>();
    }

    protected void startCleaning() {
        Thread thread = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            public void run() {
                doClean();
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    protected void doClean() {
        while (true) {
            try {
                WeakKey<TKey> lWeakKey = (WeakKey<TKey>) mQueue.remove();
                mMap.remove(lWeakKey);
            } catch (InterruptedException eInterruptedException) {
                // Ignore and retry.
            }
        }
    }

    @Override
    public TValue get(Object pKey) {
        // We cannot be sure pKey is a TKey. So use a WeakKey<Object> instead of WeakKey<TKey> and type erasure will do the rest.
        WeakValue<TValue> lWeakValue = mMap.get(new WeakKey<Object>(pKey));
        return (lWeakValue != null) ? lWeakValue.get() : null;
    }

    @Override
    public TValue put(TKey pKey, TValue pValue) {
        mMap.put(new WeakKey<TKey>(pKey, mQueue), new WeakValue<TValue>(pValue));
        return pValue;
    }

    @Override
    //@TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public Set<Entry<TKey, TValue>> entrySet() {
        Set<Entry<TKey, TValue>> entrySet = new java.util.HashSet<>();
        for (Entry<WeakKey<TKey>, WeakValue<TValue>> entry : mMap.entrySet()) {
            entrySet.add(new HashMap.SimpleEntry<TKey, TValue>(entry.getKey().get(), entry.getValue().get()));
        }
        return entrySet;
    }

    private static class WeakKey<TKey> extends WeakReference<TKey> {
        private int mHashCode;

        public WeakKey(TKey pKey) {
            super(pKey, null);
            mHashCode = pKey.hashCode();
        }

        public WeakKey(TKey pKey, ReferenceQueue<TKey> pQueue) {
            super(pKey, pQueue);
            mHashCode = pKey.hashCode();
        }

        @Override
        public int hashCode() {
            return this.mHashCode;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object pOther) {
            if (this == pOther) return true;
            if (pOther == null) return false;
            if (getClass() != pOther.getClass()) return false;

            WeakKey<TKey> lOther = (WeakKey<TKey>) pOther;
            Object lValue = get();
            return (lValue != null) && (lValue == lOther.get());
        }
    }

    private static class WeakValue<TValue> extends WeakReference<TValue> {
        public WeakValue(TValue pValue) {
            super(pValue, null);
        }
    }
}
