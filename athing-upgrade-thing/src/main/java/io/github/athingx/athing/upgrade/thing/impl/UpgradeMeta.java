package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.upgrade.thing.Upgrade;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public record UpgradeMeta(String module, String version, boolean isDiff, Upgrade.Trigger trigger, Map<String, String> extras, Set<StoreMeta> stores) {

    public record StoreMeta(URI uri, String name, long size, Sign sign) {

        public record Sign(String code, String type) {

        }

    }

}
