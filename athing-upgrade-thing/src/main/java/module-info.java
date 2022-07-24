module athing.upgrade.thing {

    exports io.github.athingx.athing.upgrade.thing;
    exports io.github.athingx.athing.upgrade.thing.builder;

    opens io.github.athingx.athing.upgrade.thing.impl.domain to com.google.gson, athing.common;

    requires athing.thing.api;
    requires com.google.gson;
    requires org.slf4j;

}