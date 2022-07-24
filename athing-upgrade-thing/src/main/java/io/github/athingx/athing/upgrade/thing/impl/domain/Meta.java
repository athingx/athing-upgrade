package io.github.athingx.athing.upgrade.thing.impl.domain;

import com.google.gson.annotations.SerializedName;

/**
 * 元数据
 */
public record Meta(
        @SerializedName("size") long size,
        @SerializedName("version") String version,
        @SerializedName("url") String upgradeURL,
        @SerializedName("md5") String upgradeMD5,
        @SerializedName("sign") String upgradeCHS,
        @SerializedName("module") String moduleId
) {

}
