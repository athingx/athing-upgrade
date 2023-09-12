package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.Thing;
import io.github.athingx.athing.thing.api.op.OpMapData;
import io.github.athingx.athing.thing.api.util.MapData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ProcessorImpl implements Processor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Thing thing;
    private final String module;
    private volatile int currentStep;
    private volatile long lastReportTimeMs;

    public ProcessorImpl(Thing thing, String module) {
        this.thing = thing;
        this.module = module;
    }

    @Override
    public CompletableFuture<Void> processing(int step, String desc) {

        // 进度不变，直接返回
        if (currentStep < 0 || (step > 0 && currentStep >= step)) {
            return completedFuture(null);
        }

        // 间隔时间小于3秒，直接返回
        if (System.currentTimeMillis() - lastReportTimeMs < 3000) {
            return completedFuture(null);
        }

        // 记录当前进度
        currentStep = step;
        lastReportTimeMs = System.currentTimeMillis();

        // 上报进度
        final var token = thing.op().genToken();
        return thing.op()
                .post("/ota/device/progress/%s/".formatted(thing.path().toURN()), new OpMapData(token, new MapData()
                        .putProperty("id", token)
                        .putProperty("params", prop -> prop
                                .putProperty("module", module)
                                .putProperty("step", step)
                                .putProperty("desc", desc)
                        )))
                .whenComplete((v, ex) ->
                        logger.debug("{}/upgrade/processing report completed! token={};step={};desc={};",
                                thing.path(), token, step, desc, ex));

    }

    @Override
    public int getCurrentStep() {
        return currentStep;
    }

    @Override
    public int computeDownloadStep(long offset, long total) {
        return (int) (offset * 70 / total);
    }

}
