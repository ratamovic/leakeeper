package com.codexperiments.leakeeper.test;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.FrameLayout;

public class TestActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle pBundle) {
        super.onCreate(pBundle);
        TestCase.sCurrentActivity = this;

        FrameLayout contentView = new FrameLayout(this);
        setContentView(contentView);
    }
}
