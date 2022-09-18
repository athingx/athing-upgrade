module athing.upgrade.platform {
    requires athing.platform.api;
    exports io.github.athingx.athing.upgrade.platform.builder;
    exports io.github.athingx.athing.upgrade.platform.message;
    opens io.github.athingx.athing.upgrade.platform.message.decoder;
}