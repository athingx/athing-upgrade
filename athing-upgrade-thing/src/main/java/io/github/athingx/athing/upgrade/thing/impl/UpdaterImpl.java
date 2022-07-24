package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.util.CompletableFutureUtils;
import io.github.athingx.athing.upgrade.thing.impl.domain.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class UpdaterImpl implements Updater {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;

    public UpdaterImpl(Thing thing) {
        this.thing = thing;
    }

    @Override
    public CompletableFuture<Void> update(String moduleId, String version) {
        final var token = thing.op().genToken();
        return thing.op()
                .data("/ota/device/inform/%s".formatted(thing.path().toURN()), new Update(
                        token,
                        moduleId,
                        version
                ))
                .whenComplete(CompletableFutureUtils.whenCompleted(
                        (v) -> logger.debug("{}/upgrade/update success, token={};module={};version={};",
                                thing.path(), token, moduleId, version),
                        (ex) -> logger.warn("{}/upgrade/update failure, token={};module={};version={};",
                                thing.path(), token, moduleId, version, ex)
                ));
    }

}
