package io.github.athingx.athing.upgrade.thing.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ThingUpgradeTestCase extends ThingUpgradeSupport {

    @Before
    public void test$before() throws Exception {
        thingUpgrade.inform("resource", "1.0.0").get();
    }

    @Test
    public void test$thing$upgrade$update() throws Exception {
        thingUpgrade.update("resource").get();
        final var upgrade = queue.take();
        Assert.assertNotNull(upgrade);
        Assert.assertEquals("resource", upgrade.module());
        Assert.assertNotNull(upgrade.version());
        Assert.assertFalse(upgrade.stores().isEmpty());

        for (final var store : upgrade.stores()) {
            Assert.assertNotNull(store.uri());
            Assert.assertNotNull(store.name());
            Assert.assertTrue(store.total() > 0);
            Assert.assertNotNull(store.persistence().persist(false).get());
        }
    }

    @Test
    public void test$inform() throws Exception {
        thingUpgrade.inform("resource", "1.0.0").get();
    }

}
