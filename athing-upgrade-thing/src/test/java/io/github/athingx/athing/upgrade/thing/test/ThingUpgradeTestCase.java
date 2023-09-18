package io.github.athingx.athing.upgrade.thing.test;

import io.github.athingx.athing.upgrade.thing.UpgradeListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ThingUpgradeTestCase extends ThingUpgradeSupport {

    private static final String RES_HTTP_S = "resource-http-s";
    private static final String RES_HTTP_M = "resource-http-m";
    private static final String RES_MQTT_D = "resource-mqtt-d";
    private static final String RES_MQTT_F = "resource-mqtt-f";

    @Before
    public void test$before() throws Exception {
        // thingUpgrade.inform(RES_HTTP_S, "1.0.0").get();
        thingUpgrade.inform(RES_HTTP_M, "1.0.0").get();
        // thingUpgrade.inform(RES_MQTT_D, "1.0.0").get();
        // thingUpgrade.inform(RES_MQTT_F, "1.0.0").get();
    }

    @Test
    public void test$$update$success() throws Exception {
        // update(RES_MQTT_F);
        // update(RES_MQTT_D);
        // update(RES_HTTP_S);
        update(RES_HTTP_M);
    }

    private void update(String module) throws Exception {
        thingUpgrade.update(module).get();
        final var upgradeOp = queue.take();
        final var upgrade = upgradeOp.upgrade();
        Assert.assertEquals(module, upgrade.module());
        Assert.assertFalse(upgrade.stores().isEmpty());
        for (final var store : upgrade.stores()) {
            Assert.assertNotNull(store.uri());
            Assert.assertNotNull(store.name());
            Assert.assertTrue(store.total() > 0);
            Assert.assertNotNull(store.persistence().persist().get());
        }
        upgradeOp.future().complete(UpgradeListener.State.UPGRADE_COMPLETED);
    }

    @Test
    public void test$inform() throws Exception {
        thingUpgrade.inform(RES_HTTP_S, "1.0.0").get();
        thingUpgrade.inform(RES_HTTP_M, "1.0.0").get();
        thingUpgrade.inform(RES_MQTT_D, "1.0.0").get();
        thingUpgrade.inform(RES_MQTT_F, "1.0.0").get();
    }

}
