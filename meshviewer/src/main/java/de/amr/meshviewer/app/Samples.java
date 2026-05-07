/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.app;

import de.amr.meshviewer.MeshSelection;
import de.amr.meshviewer.SampleInfo;
import de.amr.meshviewer.SampleInitSettings;

public interface Samples {

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
            new SampleInitSettings(-10, 0, 0, 0, MeshSelection.ALL_GROUPS, false, true)
        ),

        new SampleInfo(
            "Scooter",
            "A. Kator, J. Legaz",
            "Scooter-smgrps.obj",
            "/models/scooter/",
            "https://www.sweethome3d.com/wp-content/themes/sweet-home-3d/theme/assets/models/katorlegaz/scooter.zip",
            "https://www.sweethome3d.com/free-3d-models",
            "CCA 3.0",
            "https://github.com/AlmasB/javafx3d-samples/blob/master/LICENSE-Scooter.txt",
            new SampleInitSettings(-2, 0, 0, 0, MeshSelection.ALL_MATERIALS, false, true)
        ),

        new SampleInfo(
            "Toy Train",
            "RegusTtef",
            "toyTrain.obj",
            "/models/toy_train/",
            null,
            null,
            "CC 3.0",
            "https://creativecommons.org/licenses/by/3.0/",
            new SampleInitSettings(-30, 0, 0, 0, MeshSelection.ALL_MATERIALS, false, true)
        ),

        new SampleInfo(
            "Alien Animal",
            "Dennis Haupt",
            "Alien Animal.obj",
            "/models/alien_animal/",
            "https://free3d.com/3d-model/alien-animal-218186.html",
            "https://free3d.com/3d-model/alien-animal-218186.html",
            "LICENSE?",
            null, // TODO
            new SampleInitSettings(-50, 0, 0, 0, MeshSelection.ALL_MATERIALS, false, true)
        ),

        new SampleInfo(
            "Beagle",
            "AUTHOR TODO",
            "13041_Beagle_v1_L1.obj",
            "/models/beagle/",
            null,
            null, // TODO
            "LICENSE?",
            null, // TODO
            new SampleInitSettings(-150, 0, 0, 0, MeshSelection.ALL_MATERIALS, false, false)
        ),

        new SampleInfo(
            "Aya",
            "AUTHOR TODO",
            "091_W_Aya_100K.obj",
            "/models/aya_japanese_girl/",
            null,
            null, // TODO
            "LICENSE?",
            null, // TODO
            new SampleInitSettings(-2800, 0, 0, 0, MeshSelection.ALL_MATERIALS, false, true)
        ),

        new SampleInfo(
            "Datsun 280Z",
            "Martin Trafas",
            "Datsun_280Z.obj",
            "/models/datsun_280Z/",
            "https://sketchfab.com/3d-models/free-datsun-280z-0789ab2ece9442de94b3c41595e0ecbd",
            "https://sketchfab.com/TinoD2",
            "CC Attribution",
            "https://creativecommons.org/licenses/by/4.0/",
            new SampleInitSettings(-4, 0, 0, 0, MeshSelection.ALL_MATERIALS, false, true)
        ),

        new SampleInfo("Pac-Man",
            "Gianmarco Cavallaccio",
            "pacman.obj",
            "/models/pacman/",
            "https://sketchfab.com/3d-models/pac-man-9b2fd5bc82ba4212895fd0b753a4df09",
            "https://sketchfab.com/GianmArt",
            "CC Attribution",
            "https://creativecommons.org/licenses/by/4.0/",
            new SampleInitSettings(-42, 0, 0, 0, MeshSelection.ALL_MATERIALS, true, true)
        ),
    };
}
