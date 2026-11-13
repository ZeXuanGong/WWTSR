module com.wwtsr {
    requires javafx.controls;
    requires java.desktop;
    requires java.management;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    exports com.wwtsr.app to javafx.graphics;
    exports com.wwtsr.pet;
    exports com.wwtsr.assets;
    exports com.wwtsr.view;
    exports com.wwtsr.input;
    exports com.wwtsr.stats;
    exports com.wwtsr.settings;
    exports com.wwtsr.menu;
    exports com.wwtsr.i18n;
    exports com.wwtsr.platform;
    exports com.wwtsr.tray;
    exports com.wwtsr.io;
    exports com.wwtsr.chest;
    exports com.wwtsr.collect;
    exports com.wwtsr.player;
    exports com.wwtsr.panel;

    // JNA calls the native callbacks in these packages by reflection.
    opens com.wwtsr.input to com.sun.jna;
    opens com.wwtsr.platform to com.sun.jna;
}
