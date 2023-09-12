package io.github.athingx.athing.upgrade.thing.impl;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public interface Downloader {

    CompletableFuture<File> download(UpgradeMeta meta, UpgradeMeta.StoreMeta sMeta, Downloading downloading);

    interface Downloading {

        void onDownload(long total, long downloaded);

    }

}
