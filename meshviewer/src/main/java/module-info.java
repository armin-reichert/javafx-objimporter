module de.amr.meshviewer {
    requires javafx.controls;
    requires org.tinylog.api;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires de.amr.objparser;
    requires de.amr.meshbuilder;

    exports de.amr.meshviewer;
    exports de.amr.meshviewer.app;
    exports de.amr.samples;
}