module athing.upgrade.thing {

    exports io.github.athingx.athing.upgrade.thing;
    exports io.github.athingx.athing.upgrade.thing.impl;

    requires transitive athing.thing.api;
    requires com.google.gson;
    requires org.slf4j;
    requires java.net.http;

}