package io.github.athingx.athing.upgrade.thing.builder;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import io.github.athingx.athing.upgrade.thing.impl.InformerImpl;
import io.github.athingx.athing.upgrade.thing.impl.ThingUpgradeImpl;
import io.github.athingx.athing.upgrade.thing.impl.binding.BindForPull;
import io.github.athingx.athing.upgrade.thing.impl.binding.BindForPush;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.athingx.athing.thing.api.function.CompletableFutureFn.tryCatchComplete;

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

        final var group = thing.op().binding();
        final var updater = new InformerImpl(thing);
        group.bindFor(new BindForPush(thing, option, listeners, updater));
        final var pullCallFuture = group.bindFor(new BindForPull(thing, option));

        return group
                .commit()
                .thenCompose(binder -> tryCatchComplete(() -> new ThingUpgradeImpl(
                        thing,
                        updater,
                        listeners,
                        pullCallFuture.get()
                )));
    }

}
