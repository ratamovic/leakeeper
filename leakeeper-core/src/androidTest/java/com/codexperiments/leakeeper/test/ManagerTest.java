package com.codexperiments.leakeeper.test;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.support.v4.app.FragmentActivity;

public class ManagerTest extends ActivityInstrumentationTestCase2<TestActivity> {
    public ManagerTest() {
        super(TestActivity.class);
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
