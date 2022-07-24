package io.github.athingx.athing.upgrade.thing.builder;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import io.github.athingx.athing.upgrade.thing.impl.ThingUpgradeImpl;
import io.github.athingx.athing.upgrade.thing.impl.UpdaterImpl;
import io.github.athingx.athing.upgrade.thing.impl.binding.BindingForPush;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ThingUpgradeBuilder {

    private ThingUpgradeOption option = new ThingUpgradeOption();
    private UpgradeListener listener;

    public ThingUpgradeBuilder option(ThingUpgradeOption option) {
        this.option = option;
        return this;
    }

    public ThingUpgradeBuilder listener(UpgradeListener listener) {
        this.listener = listener;
        return this;
    }


    public CompletableFuture<ThingUpgrade> build(Thing thing) {

        final Set<UpgradeListener> listeners = ConcurrentHashMap.newKeySet();
        if (null != listener) {
            listeners.add(listener);
        }

        final var group = thing.op().group();
        final var updater = new UpdaterImpl(thing);
        group.binding(new BindingForPush(thing, option, listeners, updater));

        return group
                .commit()
                .thenCompose(binder -> completedFuture(new ThingUpgradeImpl(updater, listeners)));
    }

}
