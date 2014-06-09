package com.codexperiments.leakeeper.test;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.support.v4.app.FragmentActivity;

/**
 * TODO Failure cases.
 * 
 * TODO keepResultOnHold cases.
 */
public class TaskManagerTest extends ActivityInstrumentationTestCase2<TaskActivity> {
    public TaskManagerTest() {
        super(TaskActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }


    public void testExecute() throws InterruptedException {
        Log.e("XXX", "===================================================");
        FragmentActivity toto;
        toto=getActivity();
        toto.toString();
    }
}
