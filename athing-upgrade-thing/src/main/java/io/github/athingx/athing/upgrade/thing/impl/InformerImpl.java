package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpMapData;
import io.github.athingx.athing.thing.api.util.MapData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class InformerImpl implements Informer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;

    public InformerImpl(Thing thing) {
        this.thing = thing;
    }

    @Override
    public CompletableFuture<Void> inform(String module, String version) {
        final var token = thing.op().genToken();
        final var data = new OpMapData(token, new MapData()
                .putProperty("id", token)
                .putProperty("params", prop -> prop
                        .putProperty("module", module)
                        .putProperty("version", version)
                )
        );
        return thing.op()
                .post("/ota/device/inform/%s".formatted(thing.path().toURN()), data)
                .whenComplete((v, ex) -> logger.debug("{}/upgrade/inform completed, token={};module={};version={};",
                        thing.path(),
                        token,
                        module,
                        version,
                        ex
                ));
    }

}
