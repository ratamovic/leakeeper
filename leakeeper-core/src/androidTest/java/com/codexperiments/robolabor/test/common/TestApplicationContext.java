package com.codexperiments.robolabor.test.common;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.support.v4.app.Fragment;
import android.util.Log;

public class TestApplicationContext
{
    private List<Object> mManagers;

    public static TestApplicationContext from(Activity pActivity)
    {
        TestApplication lApplication = (TestApplication.getInstance());
        // We assume all activities will need to get the Application context during initialization (to get a manager or
        // something). Thus, at this point in the code, we can assume a new activity has just been created and so register it.
        // I see no other way to save the current activity (except making registration explicit since here it's a bit implicit).
        lApplication.setCurrentActivity(pActivity);
        return lApplication.provideContext();
    }

    public static TestApplicationContext from(Fragment pFragment)
    {
        Activity lActivity = pFragment.getActivity();
        TestApplication lApplication = (TestApplication.getInstance());
        return lApplication.provideContext();
    }

    public static TestApplicationContext from(android.app.Service pService)
    {
        TestApplication lApplication = (TestApplication.getInstance());
        return lApplication.provideContext();
    }

    public TestApplicationContext(Application pApplication)
    {
        super();
        mManagers = new ArrayList<Object>(20);
    }

    public void registerManager(Object pManager)
    {
        Log.e("=====", "registerManager");
        mManagers.add(pManager);
    }

    public void removeManagers()
    {
        mManagers.clear();
    }

    @SuppressWarnings("unchecked")
    public <TManager> TManager getManager(Class<TManager> pManagerClass)
    {
        Log.e("=====", "getManager");
        for (Object iManager : mManagers) {
            if (pManagerClass.isInstance(iManager)) {
                return (TManager) iManager;
            }
        }
        Log.e("=====", "getManagerException");
        throw TestException.unknownManager(pManagerClass);
    }


    public interface Provider
    {
        TestApplicationContext provideContext();
    }
}
