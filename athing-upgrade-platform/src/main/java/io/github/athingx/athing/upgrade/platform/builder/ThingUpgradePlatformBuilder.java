package io.github.athingx.athing.upgrade.platform.builder;

import io.github.athingx.athing.platform.api.ThingPlatform;
import io.github.athingx.athing.upgrade.platform.message.decoder.ThingModuleInformMessageDecoder;
import io.github.athingx.athing.upgrade.platform.message.decoder.ThingUpgradeProgressMessageDecoder;
import io.github.athingx.athing.upgrade.platform.message.decoder.ThingUpgradeStatusMessageDecoder;

public class ThingUpgradePlatformBuilder {

    public void build(ThingPlatform platform) {
        platform.register(new ThingModuleInformMessageDecoder());
        platform.register(new ThingUpgradeProgressMessageDecoder());
        platform.register(new ThingUpgradeStatusMessageDecoder());
    }

}
