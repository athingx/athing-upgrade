package io.github.athingx.athing.upgrade.thing.test;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.ThingPath;
import io.github.athingx.athing.thing.builder.ThingBuilder;
import io.github.athingx.athing.thing.builder.mqtt.AliyunMqttClientFactory;
import io.github.athingx.athing.upgrade.thing.ThingUpgrade;
import io.github.athingx.athing.upgrade.thing.builder.ThingUpgradeBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ThingUpgradeSupport implements LoadingProperties {

    protected static volatile Thing thing;
    protected static volatile ThingUpgrade thingUpgrade;

    @BeforeClass
    public static void _before() throws Exception {

        thing = new ThingBuilder(new ThingPath(PRODUCT_ID, THING_ID))
                .clientFactory(new AliyunMqttClientFactory()
                        .remote(THING_REMOTE)
                        .secret(THING_SECRET))
                .build();

        thingUpgrade = new ThingUpgradeBuilder()
                .build(thing)
                .get();

    }

    @AfterClass
    public static void _after() {
        thing.destroy();
    }

}
