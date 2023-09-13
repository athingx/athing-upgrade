package io.github.athingx.athing.upgrade.thing.test;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.ThingPath;
import io.github.athingx.athing.thing.builder.ThingBuilder;
import io.github.athingx.athing.thing.builder.client.DefaultMqttClientFactory;
import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.ThingUpgradeInstaller;
import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class ThingUpgradeSupport implements LoadingProperties {

    protected static volatile Thing thing;
    protected static volatile ThingUpgrade thingUpgrade;
    protected static final BlockingQueue<Upgrade> queue = new LinkedBlockingQueue<>();

    @BeforeClass
    public static void _before() throws Exception {

        thing = new ThingBuilder(new ThingPath(PRODUCT_ID, THING_ID))
                .client(new DefaultMqttClientFactory()
                        .remote(THING_REMOTE)
                        .secret(THING_SECRET))
                .build();

        final var installer = new ThingUpgradeInstaller()
                .listener(upgrade -> {
                    if (upgrade.trigger() == Upgrade.Trigger.PULL) {
                        Assert.assertTrue(queue.offer(upgrade));
                    }
                    return CompletableFuture.completedFuture(UpgradeListener.State.UPGRADE_LATER);
                });

        thingUpgrade = thing.install(installer)
                .get();

    }

    @AfterClass
    public static void _after() {
        thing.destroy();
    }

}
