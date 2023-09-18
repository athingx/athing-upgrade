package io.github.athingx.athing.upgrade.thing.test;

import io.github.athingx.athing.upgrade.thing.Upgrade;
import io.github.athingx.athing.upgrade.thing.UpgradeListener;

import java.util.concurrent.CompletableFuture;

public record UpgradeOp(Upgrade upgrade, CompletableFuture<UpgradeListener.State> future) {
}
