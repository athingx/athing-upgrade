package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.upgrade.thing.impl.domain.Process;
import io.github.athingx.athing.upgrade.thing.impl.domain.Push;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.athingx.athing.thing.api.util.CompletableFutureUtils.whenCompleted;

public class UpgradeProcessorImpl implements UpgradeProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;
    private final Push push;
    private volatile int current;

    public UpgradeProcessorImpl(Thing thing, Push push) {
        this.thing = thing;
        this.push = push;
    }

    @Override
    public void processing(int step, String desc) {

        // 如果当前步骤已经错误，或者已经大于上报步骤，则不执行上报
        if (current < 0 || current >= step) {
            return;
        }

        current = step;
        final var token = thing.op().genToken();
        final var meta = push.meta();
        final var process = new Process(token, meta.moduleId(), step, desc);
        thing.op().data("/ota/device/progress/%s/".formatted(thing.path().toURN()), process)
                .whenComplete(whenCompleted(
                        (v) -> logger.debug("{}/upgrade/process processing success, token={};push={};module={};version={};step={};",
                                thing.path(), token, push.token(), meta.moduleId(), meta.version(), step),
                        (ex) -> logger.warn("{}/upgrade/process processing failure, token={};push={};module={};version={};step={};",
                                thing.path(), token, push.token(), meta.moduleId(), meta.version(), step)
                ));

    }

}
