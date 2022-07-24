package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpCaller;
import io.github.athingx.athing.thing.api.op.OpReply;
import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import io.github.athingx.athing.upgrade.thing.impl.domain.Pull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ThingUpgradeImpl implements ThingUpgrade {

    private final Thing thing;
    private final Informer informer;
    private final Set<UpgradeListener> listeners;
    private final OpCaller<Pull, OpReply<Upgrade>> pullCaller;


    public ThingUpgradeImpl(Thing thing, Informer informer, Set<UpgradeListener> listeners, OpCaller<Pull, OpReply<Upgrade>> pullCaller) {
        this.thing = thing;
        this.informer = informer;
        this.listeners = listeners;
        this.pullCaller = pullCaller;
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
    public CompletableFuture<Void> inform(String moduleId, String version) {
        return informer.inform(moduleId, version);
    }

    @Override
    public CompletableFuture<Void> update(String moduleId) {
        return fetch(moduleId)
                .thenApply(OpReply::data)
                .thenApply(upgrade -> {
                    listeners.forEach(listener -> listener.apply(upgrade));
                    return upgrade;
                })
                .thenCompose(upgrade -> informer.inform(
                        upgrade.getModuleId(),
                        upgrade.getVersion()
                ));
    }

    @Override
    public CompletableFuture<OpReply<Upgrade>> fetch(String moduleId) {
        final var token = thing.op().genToken();
        return pullCaller.call(
                "/sys/%s/thing/ota/firmware/get".formatted(thing.path().toURN()),
                new Pull(token, moduleId)
        );
    }

}
