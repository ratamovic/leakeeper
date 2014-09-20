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
        mMap = new ConcurrentHashMap<>(pCapacity);
        mQueue = new ReferenceQueue<>();
    }

    protected void startCleaning() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                doClean();
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("unchecked")
    protected void doClean() {
        while (true) {
            try {
                WeakKey<TKey> weakKey = (WeakKey<TKey>) mQueue.remove();
                mMap.remove(weakKey);
            } catch (InterruptedException eInterruptedException) {
                // Ignore and retry.
            }
        }
    }

    @Override
    public TValue get(Object pKey) {
        // We cannot be sure pKey is a TKey. So use a WeakKey<Object> instead of WeakKey<TKey> and type erasure will do the rest.
        WeakValue<TValue> weakValue = mMap.get(new WeakKey<>(pKey));
        return (weakValue != null) ? weakValue.get() : null;
    }

    @Override
    public TValue put(TKey pKey, TValue pValue) {
        mMap.put(new WeakKey<>(pKey, mQueue), new WeakValue<>(pValue));
        return pValue;
    }

    @Override
    public Set<Entry<TKey, TValue>> entrySet() {
        Set<Entry<TKey, TValue>> entrySet = new java.util.HashSet<>();
        for (Entry<WeakKey<TKey>, WeakValue<TValue>> entry : mMap.entrySet()) {
            entrySet.add(new HashMap.SimpleEntry<>(entry.getKey().get(), entry.getValue().get()));
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

            WeakKey<TKey> other = (WeakKey<TKey>) pOther;
            Object value = get();
            return (value != null) && (value == other.get());
        }
    }

    private static class WeakValue<TValue> extends WeakReference<TValue> {
        public WeakValue(TValue pValue) {
            super(pValue, null);
        }
    }
}
