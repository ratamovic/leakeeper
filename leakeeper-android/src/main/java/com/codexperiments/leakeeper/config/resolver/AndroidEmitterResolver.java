package com.codexperiments.leakeeper.config.resolver;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;

/**
 * Example configuration that handles basic Android components: Activity and Fragments.
 */
public class AndroidEmitterResolver implements EmitterResolver {
    private final Class<?> mFragmentClass;
    private final Class<?> mFragmentCompatClass;

    public AndroidEmitterResolver() {
        mFragmentClass = tryToLoadClass("android.app.Fragment");
        mFragmentCompatClass = tryToLoadClass("android.support.v4.app.Fragment");
    }

    private Class<?> tryToLoadClass(String pName) {
        try {
            return Class.forName(pName, false, getClass().getClassLoader());
        } catch (ClassNotFoundException eClassNotFoundException) {
            // Current application doesn't embed compatibility library.
            return null;
        }
    }

    @Override
    public Object resolveEmitterId(Object pEmitter) {

        if (pEmitter instanceof Activity) {
            return resolveActivityId((Activity) pEmitter);
        } else if (mFragmentClass != null && mFragmentClass.isInstance(pEmitter)) {
            return resolveFragmentId((android.app.Fragment) pEmitter);
        } else if (mFragmentCompatClass != null && mFragmentCompatClass.isInstance(pEmitter)) {
            return resolveFragmentId((android.support.v4.app.Fragment) pEmitter);
        }
        return null;
    }

    /**
     * Typically, an Android Activity is identified by its class type: if we start a task X in an activity of type A, navigate to
     * an Activity of type B and finally go back to an activity of type A (which could have been recreated meanwhile), then we
     * want any pending task emitted by the 1st Activity A to be attached again to any further Activity of the same type.
     * 
     * @param pActivity Activity to find the Id of.
     * @return Activity class.
     */
    protected Object resolveActivityId(Activity pActivity) {
        return pActivity.getClass();
    }

    /**
     * Typically, an Android Fragment is identified either by an Id (the Id of the component it is inserted in) or a Tag ( which
     * is a String).If none of these elements is available, then Fragment's class is used instead.
     * 
     * @param pFragment Fragment to find the Id of.
     * @return Fragment Id if not 0, Fragment Tag if not empty or else its Fragment class.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected Object resolveFragmentId(android.app.Fragment pFragment) {
        if (pFragment.getId() > 0) {
            return pFragment.getId();
        } else if (pFragment.getTag() != null && !TextUtils.isEmpty(pFragment.getTag())) {
            return pFragment.getTag();
        } else {
            return pFragment.getClass();
        }
    }

    /**
     * Same as the homonym method but for fragments from the compatiblity library.
     */
    protected Object resolveFragmentId(android.support.v4.app.Fragment pFragment) {
        if (pFragment.getId() > 0) {
            return pFragment.getId();
        } else if (pFragment.getTag() != null && !TextUtils.isEmpty(pFragment.getTag())) {
            return pFragment.getTag();
        } else {
            return pFragment.getClass();
        }
    }
}
