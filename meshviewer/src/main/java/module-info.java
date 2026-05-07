module de.amr.meshviewer {
    requires javafx.controls;
    requires org.tinylog.api;
    requires de.amr.objparser;
    requires de.amr.meshbuilder;
    requires com.google.gson;

    exports de.amr.meshviewer;
    exports de.amr.meshviewer.app;
    exports de.amr.samples;
}