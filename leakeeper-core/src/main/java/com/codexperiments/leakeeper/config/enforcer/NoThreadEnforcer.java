package com.codexperiments.leakeeper.config.enforcer;

public class NoThreadEnforcer implements ThreadEnforcer {
    @Override
    public void enforce() {
        // Allowed from any thread.
    }
}