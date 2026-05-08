module de.amr.meshviewer {
    requires javafx.controls;
    requires com.google.gson;
    requires org.tinylog.api;
    requires de.amr.objparser;
    requires de.amr.meshbuilder;

    exports de.amr.meshviewer;
    exports de.amr.meshviewer.app;
    exports de.amr.meshviewer.meshtree;
    exports de.amr.samples;
    exports de.amr.meshviewer.info;
}