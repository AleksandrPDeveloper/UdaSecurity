module securityservice {
    requires java.datatransfer;
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires com.miglayout.swing;
    requires transitive imageservice;
    opens com.udacity.catpoint.data to com.google.gson;
    exports com.udacity.catpoint.data;
}