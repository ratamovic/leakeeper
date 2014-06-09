package com.codexperiments.leakeeper.test;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class TaskActivity extends FragmentActivity {
    public static Intent dying() {
        return createIntent(true, false);
    }

    private static Intent createIntent(boolean pCheckEmitterNull, boolean pStepByStep) {
        Intent intent = new Intent();
        return intent;
    }

    @Override
    protected void onCreate(Bundle pBundle) {
        super.onCreate(pBundle);
        //setContentView(R.layout.main);
    }
}
