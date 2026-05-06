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
            "https://www.cs.utah.edu/~natevm/newell_teaset/newell_teaset.zip",
            "https://graphics.cs.utah.edu/teapot/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-10, 0, 0, 0)),

        new SampleInfo(
            "Scooter",
            "A. Kator, J. Legaz",
            "Scooter-smgrps.obj",
            "/models/scooter/",
            "https://www.sweethome3d.com/wp-content/themes/sweet-home-3d/theme/assets/models/katorlegaz/scooter.zip",
            "https://www.sweethome3d.com/free-3d-models",
            "CCA 3.0",
            "https://github.com/AlmasB/javafx3d-samples/blob/master/LICENSE-Scooter.txt",
            new TransformSettings(-2, 0, 0, 0)),

        new SampleInfo(
            "Alien Animal",
            "Dennis Haupt",
            "Alien Animal.obj",
            "/models/alien_animal/",
            null,
            "https://free3d.com/3d-model/alien-animal-218186.html",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-50, 0, 0, 0)),

        new SampleInfo(
            "Beagle",
            "AUTHOR TODO",
            "13041_Beagle_v1_L1.obj",
            "/models/beagle/",
            null,
            null, // TODO
            "LICENSE?",
            null, // TODO
            new TransformSettings(-150, 0, 0, 0)),

        new SampleInfo(
            "Aya",
            "AUTHOR TODO",
            "091_W_Aya_100K.obj",
            "/models/aya_japanese_girl/",
            null,
            null, // TODO
            "LICENSE?",
            null, // TODO
            new TransformSettings(-2800, 0, 0, 0)),

        new SampleInfo(
            "Datsun 280Z",
            "AUTHOR TODO",
            "Datsun_280Z.obj",
            "/models/datsun_280Z/",
            null,
            null, // TODO
            "LICENSE?",
            null, // TODO
            new TransformSettings(-4, 0, 0, 0)),

        new SampleInfo("Pac-Man",
            "Gianmarco Cavallaccio",
            "pacman.obj",
            "/models/pacman/",
            "https://sketchfab.com/3d-models/pac-man-9b2fd5bc82ba4212895fd0b753a4df09",
            "https://sketchfab.com/GianmArt",
            "CC Attribution",
            "http://creativecommons.org/licenses/by/4.0/",
            new TransformSettings(-42, 30, 0, 0))
    };
}
