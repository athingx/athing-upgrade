package io.github.athingx.athing.upgrade.thing;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.plugin.ThingPluginInstaller;
import io.github.athingx.athing.upgrade.thing.impl.ThingUpgradeImpl;
import io.github.athingx.athing.upgrade.thing.impl.UpgradeMeta;
import io.github.athingx.athing.upgrade.thing.impl.binding.ThingOpBindingForDigger;
import io.github.athingx.athing.upgrade.thing.impl.binding.ThingOpBindingForPuller;
import io.github.athingx.athing.upgrade.thing.impl.binding.ThingOpBindingForPusher;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class ThingUpgradeInstaller implements ThingPluginInstaller<ThingUpgrade> {

    private ThingUpgradeOption option = new ThingUpgradeOption();
    private UpgradeListener listener;

    public ThingUpgradeInstaller option(ThingUpgradeOption option) {
        this.option = option;
        return this;
    }

    public ThingUpgradeInstaller listener(UpgradeListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public Meta<ThingUpgrade> meta() {
        return new Meta<>(ThingUpgrade.THING_UPGRADE_ID, ThingUpgrade.class);
    }

    @Override
    public CompletableFuture<ThingUpgrade> install(Thing thing) {
        Objects.requireNonNull(option, "option can not be null!");
        Objects.requireNonNull(listener, "listener can not be null!");
        final var queue = new LinkedBlockingQueue<UpgradeMeta>();
        final var diggerF = new ThingOpBindingForDigger(option).bind(thing);
        final var pullerF = new ThingOpBindingForPuller().bind(thing);
        final var pusherF = new ThingOpBindingForPusher(queue).bind(thing);
        return CompletableFuture.allOf(diggerF, pusherF, pullerF)
                .thenApply(v -> new ThingUpgradeImpl(
                        thing,
                        option,
                        diggerF.join(),
                        pullerF.join(),
                        pusherF.join(),
                        queue,
                        listener
                ));
    }

}
