package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpRequest;
import io.github.athingx.athing.thing.api.util.MapData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static io.github.athingx.athing.thing.api.op.Encoder.encodeJsonToBytes;
import static io.github.athingx.athing.thing.api.op.Encoder.encodeOpRequestToJson;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 模块版本通告器实现
 */
public class InformerImpl implements Informer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;

    public InformerImpl(Thing thing) {
        this.thing = thing;
    }

    @Override
    public CompletableFuture<Void> inform(String module, String version) {
        final var token = thing.op().genToken();
        final var request = new OpRequest<>(
                thing.op().genToken(),
                new MapData()
                        .putProperty("module", module)
                        .putProperty("version", version)

        );
        return thing.op()
                .encode(encodeJsonToBytes(UTF_8))
                .encode(encodeOpRequestToJson(MapData.class))
                .post("/ota/device/inform/%s".formatted(thing.path().toURN()), request)
                .whenComplete((v, ex) -> logger.debug("{}/upgrade/inform completed, token={};module={};version={};",
                        thing.path(),
                        token,
                        module,
                        version,
                        ex
                ));
    }

}
