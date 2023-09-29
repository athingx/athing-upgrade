package io.github.athingx.athing.upgrade.thing.test;

import io.github.athingx.athing.upgrade.thing.ThingUpgradeOption;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ThingUpgradeTestCase extends ThingUpgradeSupport {

    private static final String RES_HTTP_S = "resource-http-s";
    private static final String RES_HTTP_M = "resource-http-m";
    private static final String RES_MQTT_D = "resource-mqtt-d";
    private static final String RES_MQTT_F = "resource-mqtt-f";


    private boolean delete(File file) {
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            final var files = file.listFiles();
            if (null == files) {
                return file.delete();
            }
            for (final var sub : files) {
                if (!delete(sub)) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    @Test
    public void test$$update$success() throws Exception {

        Assert.assertTrue(delete(new ThingUpgradeOption().getUpgradeDir()));

        // update(RES_MQTT_D);
        update(RES_MQTT_F);
        // update(RES_HTTP_S);
        // update(RES_HTTP_M);
    }

    private void update(String module) throws Exception {

        thingUpgrade.inform(module, "1.0.0");
        Thread.sleep(1000 * 10L);

        thingUpgrade.update(module).get();
        final var upgrade = queue.take();
        Assert.assertEquals(module, upgrade.module());
        Assert.assertFalse(upgrade.stores().isEmpty());
        for (final var store : upgrade.stores()) {
            Assert.assertNotNull(store.uri());
            Assert.assertNotNull(store.name());
            Assert.assertTrue(store.total() > 0);
            Assert.assertNotNull(store.persistence().persist(true).get());
        }
        upgrade.application().apply();
    }

    @Test
    public void test$inform() throws Exception {
        thingUpgrade.inform(RES_HTTP_S, "1.0.0").get();
        thingUpgrade.inform(RES_HTTP_M, "1.0.0").get();
        thingUpgrade.inform(RES_MQTT_D, "1.0.0").get();
        thingUpgrade.inform(RES_MQTT_F, "1.0.0").get();
    }

}
