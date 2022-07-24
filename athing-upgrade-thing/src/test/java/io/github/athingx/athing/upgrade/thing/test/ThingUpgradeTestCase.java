package io.github.athingx.athing.upgrade.thing.test;

import io.github.athingx.athing.upgrade.thing.Upgrade;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThingUpgradeTestCase extends ThingUpgradeSupport {

    @Before
    public void test$before() throws Exception {
        thingUpgrade.inform("resource", "1.0.0").get();
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

        final BlockingQueue<Upgrade> queue = new LinkedBlockingQueue<>();
        thingUpgrade.appendListener(upgrade -> {
            while (true) {
                if (queue.offer(upgrade)) {
                    break;
                }
            }
        });
        thingUpgrade.update("resource").get();
        final Upgrade upgrade = queue.take();
        Assert.assertNotNull(upgrade);
        Assert.assertEquals("resource", upgrade.getModuleId());
        Assert.assertNotNull(upgrade.getVersion());
        Assert.assertTrue(upgrade.getSize() > 0);
        Assert.assertNotNull(upgrade.getFile().get());

    }

}
