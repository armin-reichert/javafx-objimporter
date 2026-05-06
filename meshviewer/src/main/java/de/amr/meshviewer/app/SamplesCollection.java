package de.amr.meshviewer.app;

import de.amr.meshviewer.SampleInfo;
import de.amr.meshviewer.TransformSettings;

public interface SamplesCollection {

    SampleInfo[] SAMPLES = {
        new SampleInfo(
            "Teapot",
            "Martin Newell, Jim Blinn",
            "teapot.obj",
            "/models/newell_teaset/",
            "https://graphics.cs.utah.edu/teapot/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-10, 0, 0, 0)),

        new SampleInfo(
            "Scooter",
            "Andrew Kator, Jennifer Legaz",
            "Scooter-normals.obj",
            "/models/scooter/",
            "https://www.sweethome3d.com/free-3d-models",
            "CCA 3.0",
            "https://github.com/AlmasB/javafx3d-samples/blob/master/LICENSE-Scooter.txt",
            new TransformSettings(-2, 0, 0, 0)),

        new SampleInfo(
            "Scooter (Smooth)",
            "AUTHOR TODO",
            "Scooter-smgrps.obj",
            "/models/scooter/",
            null, // TODO
            "LICENSE?",
            null, // TODO
            new TransformSettings(-2, 0, 0, 0)),

        new SampleInfo(
            "Alien Animal",
            "AUTHOR TODO",
            "Alien Animal.obj",
            "/models/alien_animal/",
            null, // TODO
            "LICENSE?",
            null, // TODO
            new TransformSettings(-50, 0, 0, 0)),

        new SampleInfo(
            "Beagle",
            "AUTHOR TODO",
            "13041_Beagle_v1_L1.obj",
            "/models/beagle/",
            null, // TODO
            "LICENSE?",
            null, // TODO
            new TransformSettings(-150, 0, 0, 0)),

        new SampleInfo(
            "Aya",
            "AUTHOR TODO",
            "091_W_Aya_100K.obj",
            "/models/aya_japanese_girl/",
            null, // TODO
            "LICENSE?",
            null, // TODO
            new TransformSettings(-2800, 0, 0, 0)),

        new SampleInfo(
            "Datsun 280Z",
            "AUTHOR TODO",
            "Datsun_280Z.obj",
            "/models/datsun_280Z/",
            null, // TODO
            "LICENSE?",
            null, // TODO
            new TransformSettings(-4, 0, 0, 0)),

        new SampleInfo("Pac-Man",
            "Gianmarco Cavallaccio",
            "pacman.obj",
            "/models/pacman/",
            "https://sketchfab.com/3d-models/pac-man-9b2fd5bc82ba4212895fd0b753a4df09",
            "CC Attribution",
            "http://creativecommons.org/licenses/by/4.0/",
            new TransformSettings(-42, 30, 0, 0))
    };
}
