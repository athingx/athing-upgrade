package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.upgrade.thing.Upgrade;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public record UpgradeMeta(String module, String version, boolean isDiff, Upgrade.Trigger trigger,
                          Map<String, String> extras, Set<StoreMeta> stores) {

    public record StoreMeta(String name, URI uri, long size, Info info, Sign sign) {

        public record Info(String module, String version) {

        }

        public record Sign(String code, String type) {

        }

    }

}
