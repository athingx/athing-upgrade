package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ThingUpgradeImpl implements ThingUpgrade {

    private final Updater updater;
    private final Set<UpgradeListener> listeners;


    public ThingUpgradeImpl(Updater updater, Set<UpgradeListener> listeners) {
        this.updater = updater;
        this.listeners = listeners;
    }

    @Override
    public void appendListener(UpgradeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(UpgradeListener listener) {
        listeners.add(listener);
    }

    @Override
    public CompletableFuture<Void> update(String moduleId, String version) {
        return updater.update(moduleId, version);
    }

}
