package com.codexperiments.leakeeper.config.resolver;

public class NoResolver implements EmitterResolver {
    @Override
    public Object resolveEmitterId(Object pEmitter) {
        return null;
    }
}
