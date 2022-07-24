package io.github.athingx.athing.upgrade.thing.impl.domain;

import com.google.gson.annotations.SerializedName;
import io.github.athingx.athing.thing.api.op.OpData;

public class Pull implements OpData {

    @SerializedName("id")
    private final String token;

    @SerializedName("version")
    private final String version;

    @SerializedName("method")
    private final String method;

    @SerializedName("params")
    private final Param param;

    public Pull(String token, String moduleId) {
        this.token = token;
        this.version = "1.0";
        this.method = "thing.ota.firmware.get";
        this.param = new Param(moduleId);
    }

    @Override
    public String token() {
        return token;
    }

    private record Param(
            @SerializedName("module") String module
    ) {

    }

}
