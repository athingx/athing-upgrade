package io.github.athingx.athing.upgrade.thing.impl.downloader;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

interface FileLoader {

    CompletableFuture<File> load(Executor executor, Loading loading);

    interface Loading {

        void onLoaded(long total, long loaded);

        static Loading empty() {
            return (total, loaded) -> {
            };
        }

    }

}
