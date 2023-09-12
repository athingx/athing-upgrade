package io.github.athingx.athing.upgrade.thing.impl;

import io.github.athingx.athing.thing.api.op.ThingOpBinder;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface Digger extends ThingOpBinder {

    CompletableFuture<Response> download(Request request);

    record Request(String streamId, String fileId, long total, long position, int size) {

    }

    record Response(long total, long position, ByteBuffer buffer) {

    }

}
