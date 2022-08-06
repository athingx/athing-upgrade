package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpCall;
import io.github.athingx.athing.thing.api.op.OpReply;
import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import io.github.athingx.athing.upgrade.thing.impl.domain.Pull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ThingUpgradeImpl implements ThingUpgrade {

    private final Thing thing;
    private final Informer informer;
    private final Set<UpgradeListener> listeners;
    private final OpCall<Pull, OpReply<Upgrade>> pullCall;


    public ThingUpgradeImpl(Thing thing, Informer informer, Set<UpgradeListener> listeners, OpCall<Pull, OpReply<Upgrade>> pullCall) {
        this.thing = thing;
        this.informer = informer;
        this.listeners = listeners;
        this.pullCall = pullCall;
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
                .thenCompose(upgrade -> {
                    if (upgrade.isUpdated() && !listeners.isEmpty()) {
                        listeners.forEach(listener -> listener.apply(upgrade));
                        return informer.inform(
                                upgrade.getModuleId(),
                                upgrade.getVersion()
                        );
                    } else {
                        return completedFuture(null);
                    }
                });
    }

    @Override
    public CompletableFuture<OpReply<Upgrade>> fetch(String moduleId) {
        final var token = thing.op().genToken();
        return pullCall.calling(
                "/sys/%s/thing/ota/firmware/get".formatted(thing.path().toURN()),
                new Pull(token, moduleId)
        );
    }

}
