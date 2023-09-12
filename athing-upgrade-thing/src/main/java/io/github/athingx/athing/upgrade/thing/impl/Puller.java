package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.op.ThingOpBinder;

import java.util.concurrent.CompletableFuture;

public interface Puller extends ThingOpBinder {

    CompletableFuture<UpgradeMeta> pull(String module);

}
