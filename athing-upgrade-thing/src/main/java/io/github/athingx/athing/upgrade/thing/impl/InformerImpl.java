package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.impl.domain.Inform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.thing.api.function.CompletableFutureFn.whenCompleted;

public class InformerImpl implements Informer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;

    public InformerImpl(Thing thing) {
        this.thing = thing;
    }

    @Override
    public CompletableFuture<Void> inform(String moduleId, String version) {
        final var token = thing.op().genToken();
        final var inform = new Inform(token, moduleId, version);
        return thing.op()
                .data("/ota/device/inform/%s".formatted(thing.path().toURN()), inform)
                .whenComplete(whenCompleted(
                        v -> logger.debug("{}/upgrade/inform success, token={};module={};version={};",
                                thing.path(), token, moduleId, version),
                        ex -> logger.warn("{}/upgrade/inform failure, token={};module={};version={};",
                                thing.path(), token, moduleId, version, ex)
                ));
    }

}
