package io.github.athingx.athing.upgrade.thing.test;

import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;

public class ThingUpgradeTestCase extends ThingUpgradeSupport {

    @Before
    public void test$before() throws Exception {
        thingUpgrade.inform("resource", "1.0.0").get();
        Thread.sleep(1000L * 5);
    }

    @Test
    public void test$thing$upgrade$fetch() throws Exception {

        final var reply = thingUpgrade.fetch("resource").get();
        Assert.assertTrue(reply.isOk());
        final var upgrade = reply.data();
        Assert.assertEquals("resource", upgrade.getModuleId());
        Assert.assertTrue(upgrade.getSize() > 0);
        Assert.assertNotNull(upgrade.getVersion());
        Assert.assertNotNull(upgrade.getFile());
    }

    @Test
    public void test$thing$upgrade$update() throws Exception {

        final var queue = new LinkedBlockingQueue<Upgrade>();
        final var listener = new UpgradeListener() {

            @Override
            public void apply(Upgrade upgrade) {
                while (true) {
                    if (queue.offer(upgrade)) {
                        break;
                    }
                }
            }
        };
        thingUpgrade.appendListener(listener);
        try {
            thingUpgrade.update("resource").get();
            final Upgrade upgrade = queue.take();
            Assert.assertNotNull(upgrade);
            Assert.assertEquals("resource", upgrade.getModuleId());
            Assert.assertNotNull(upgrade.getVersion());
            Assert.assertTrue(upgrade.getSize() > 0);
            Assert.assertNotNull(upgrade.getFile().get());
        } finally {
            thingUpgrade.removeListener(listener);
        }

    }

    @Ignore
    @Test
    public void test$thing$upgrade$push() throws Exception {

        final var queue = new LinkedBlockingQueue<Upgrade>();
        final var listener = new UpgradeListener() {

            @Override
            public void apply(Upgrade upgrade) {
                while (true) {
                    if (queue.offer(upgrade)) {
                        break;
                    }
                }
            }
        };
        thingUpgrade.appendListener(listener);
        try {
            final Upgrade upgrade = queue.take();
            Assert.assertNotNull(upgrade);
            Assert.assertEquals("resource", upgrade.getModuleId());
            Assert.assertNotNull(upgrade.getVersion());
            Assert.assertTrue(upgrade.getSize() > 0);
            Assert.assertNotNull(upgrade.getFile().get());
        } finally {
            thingUpgrade.removeListener(listener);
        }

    }

}
