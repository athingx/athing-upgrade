package io.github.athingx.athing.upgrade.thing.impl.util;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {

    public static void closeQuietly(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null)
                    try {
                        closeable.close();
                    } catch (IOException cause) {
                        // ignore
                    }
            }
        }
    }

}
